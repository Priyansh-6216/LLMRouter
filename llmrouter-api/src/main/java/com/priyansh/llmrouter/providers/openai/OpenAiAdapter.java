package com.priyansh.llmrouter.providers.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.api.GenerateResponse;
import com.priyansh.llmrouter.api.Usage;
import com.priyansh.llmrouter.providers.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class OpenAiAdapter implements ProviderAdapter {

    private static final String PROVIDER_NAME = "openai";
    // We will hardcode model for now if routing doesn't select one
    private static final String DEFAULT_MODEL = "gpt-4o-mini"; 

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<GenerateResponse> generate(GenerateRequest request) {
        long startTime = System.currentTimeMillis();

        OpenAiRequest openAiRequest = toOpenAiRequest(request, false);

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .map(openAiResponse -> toGenerateResponse(openAiResponse, request.getTenantId(), startTime));
    }

    @Override
    public Flux<String> generateStream(GenerateRequest request) {
        OpenAiRequest openAiRequest = toOpenAiRequest(request, true);
        String reqId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        return openAiWebClient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.equals("[DONE]"))
                .map(chunk -> {
                    try {
                        OpenAiResponse response = objectMapper.readValue(chunk, OpenAiResponse.class);
                        String text = "";
                        boolean done = false;
                        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                            OpenAiResponse.Choice choice = response.getChoices().get(0);
                            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                text = choice.getDelta().getContent();
                            }
                            if (choice.getFinish_reason() != null) {
                                done = true;
                            }
                        }
                        
                        String escapedText = text.replace("\"", "\\\"").replace("\n", "\\n");
                        String out = "data: {\"id\":\"" + reqId + "\",\"chunk\":\"" + escapedText + "\",\"provider\":\"" + PROVIDER_NAME + "\",\"model\":\"" + DEFAULT_MODEL + "\",\"done\":" + done + "}\n\n";
                        return out;
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse chunk", e);
                        return "";
                    }
                });
    }

    private OpenAiRequest toOpenAiRequest(GenerateRequest request, boolean stream) {
        return OpenAiRequest.builder()
                .model(DEFAULT_MODEL) // For Day 3, we hardcode. Later the routing engine will provide this.
                .stream(stream)
                .messages(request.getMessages().stream()
                        .map(m -> OpenAiRequest.Message.builder()
                                .role(m.getRole())
                                .content(m.getContent())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private GenerateResponse toGenerateResponse(OpenAiResponse openAiResponse, String tenantId, long startTime) {
        long latencyMs = System.currentTimeMillis() - startTime;
        
        String text = "";
        if (openAiResponse.getChoices() != null && !openAiResponse.getChoices().isEmpty()) {
            text = openAiResponse.getChoices().get(0).getMessage().getContent();
        }

        int inputTokens = 0;
        int outputTokens = 0;
        if (openAiResponse.getUsage() != null) {
            inputTokens = openAiResponse.getUsage().getPrompt_tokens();
            outputTokens = openAiResponse.getUsage().getCompletion_tokens();
        }

        return GenerateResponse.builder()
                .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                .provider(PROVIDER_NAME)
                .model(openAiResponse.getModel())
                .latencyMs(latencyMs)
                .cacheHit(false)
                .fallbackUsed(false)
                .usage(Usage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .estimatedCostUsd(calculateCost(inputTokens, outputTokens))
                        .build())
                .output(Map.of("text", text))
                .build();
    }

    private Double calculateCost(int inputTokens, int outputTokens) {
        // gpt-4o-mini placeholder cost
        return (inputTokens * 0.00015 / 1000.0) + (outputTokens * 0.0006 / 1000.0);
    }

    @Override
    public Mono<Boolean> checkHealth() {
        return openAiWebClient.get()
                .uri("/models")
                .retrieve()
                .toBodilessEntity()
                .map(entity -> entity.getStatusCode().is2xxSuccessful())
                .onErrorResume(e -> {
                    log.warn("OpenAI health check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
