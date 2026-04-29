package com.priyansh.llmrouter.providers.vllm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.api.GenerateResponse;
import com.priyansh.llmrouter.api.Usage;
import com.priyansh.llmrouter.providers.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VllmAdapter implements ProviderAdapter {

    private static final String PROVIDER_NAME = "vllm";
    
    @Value("${vllm.api.model:meta-llama/Llama-3-8B-Instruct}")
    private String defaultModel;

    private final WebClient vllmWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<GenerateResponse> generate(GenerateRequest request) {
        long startTime = System.currentTimeMillis();

        VllmRequest vllmRequest = toVllmRequest(request, false);

        return vllmWebClient.post()
                .uri("/chat/completions")
                .bodyValue(vllmRequest)
                .retrieve()
                .bodyToMono(VllmResponse.class)
                .map(vllmResponse -> toGenerateResponse(vllmResponse, request.getTenantId(), startTime));
    }

    @Override
    public Flux<String> generateStream(GenerateRequest request) {
        VllmRequest vllmRequest = toVllmRequest(request, true);
        String reqId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        return vllmWebClient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(vllmRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.trim().isEmpty() && !chunk.contains("[DONE]"))
                .map(chunk -> {
                    try {
                        // vLLM chunks often start with "data: "
                        String jsonChunk = chunk.startsWith("data: ") ? chunk.substring(6) : chunk;
                        VllmResponse response = objectMapper.readValue(jsonChunk, VllmResponse.class);
                        String text = "";
                        boolean done = false;
                        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                            VllmResponse.Choice choice = response.getChoices().get(0);
                            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                text = choice.getDelta().getContent();
                            }
                            if (choice.getFinish_reason() != null) {
                                done = true;
                            }
                        }
                        
                        String escapedText = text.replace("\"", "\\\"").replace("\n", "\\n");
                        String out = "data: {\"id\":\"" + reqId + "\",\"chunk\":\"" + escapedText + "\",\"provider\":\"" + PROVIDER_NAME + "\",\"model\":\"" + defaultModel + "\",\"done\":" + done + "}\n\n";
                        return out;
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse vLLM chunk: {}", chunk, e);
                        return "";
                    }
                });
    }

    private VllmRequest toVllmRequest(GenerateRequest request, boolean stream) {
        return VllmRequest.builder()
                .model(defaultModel)
                .stream(stream)
                .messages(request.getMessages().stream()
                        .map(m -> VllmRequest.Message.builder()
                                .role(m.getRole())
                                .content(m.getContent())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private GenerateResponse toGenerateResponse(VllmResponse vllmResponse, String tenantId, long startTime) {
        long latencyMs = System.currentTimeMillis() - startTime;
        
        String text = "";
        if (vllmResponse.getChoices() != null && !vllmResponse.getChoices().isEmpty()) {
            text = vllmResponse.getChoices().get(0).getMessage().getContent();
        }

        int inputTokens = 0;
        int outputTokens = 0;
        if (vllmResponse.getUsage() != null) {
            inputTokens = vllmResponse.getUsage().getPrompt_tokens();
            outputTokens = vllmResponse.getUsage().getCompletion_tokens();
        }

        return GenerateResponse.builder()
                .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                .provider(PROVIDER_NAME)
                .model(vllmResponse.getModel())
                .latencyMs(latencyMs)
                .cacheHit(false)
                .fallbackUsed(false)
                .usage(Usage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .estimatedCostUsd(0.0) // vLLM is self-hosted, so usually 0 USD direct cost
                        .build())
                .output(Map.of("text", text))
                .build();
    }

    @Override
    public Mono<Boolean> checkHealth() {
        return vllmWebClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .map(entity -> entity.getStatusCode().is2xxSuccessful())
                .onErrorResume(e -> {
                    log.warn("vLLM health check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
