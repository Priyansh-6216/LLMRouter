package com.priyansh.llmrouter.providers.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnthropicAdapter implements ProviderAdapter {

    private static final String PROVIDER_NAME = "anthropic";
    private static final String DEFAULT_MODEL = "claude-3-haiku-20240307";

    private final WebClient anthropicWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Mono<GenerateResponse> generate(GenerateRequest request, String model) {
        long startTime = System.currentTimeMillis();

        AnthropicRequest anthropicRequest = toAnthropicRequest(request, model, false);

        return anthropicWebClient.post()
                .uri("/messages")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToMono(AnthropicResponse.class)
                .map(anthropicResponse -> toGenerateResponse(anthropicResponse, request.getTenantId(), startTime));
    }

    @Override
    public Flux<String> generateStream(GenerateRequest request, String model) {
        AnthropicRequest anthropicRequest = toAnthropicRequest(request, model, true);
        String reqId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        return anthropicWebClient.post()
                .uri("/messages")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToFlux(String.class)
                // Filter out SSE event lines like "event: message_start" and only process "data: ..."
                .filter(chunk -> chunk.startsWith("data: "))
                .map(chunk -> chunk.substring(6).trim()) // Remove "data: " prefix
                .map(jsonData -> {
                    try {
                        JsonNode root = objectMapper.readTree(jsonData);
                        String type = root.path("type").asText("");
                        
                        String text = "";
                        boolean done = false;

                        if ("content_block_delta".equals(type)) {
                            text = root.path("delta").path("text").asText("");
                        } else if ("message_stop".equals(type)) {
                            done = true;
                        } else {
                            // Other events like message_start, content_block_start, ping
                            // We ignore them for the text output
                            if (!type.equals("message_start") && !type.equals("content_block_start")) {
                                return ""; 
                            }
                        }

                        // if both empty and not done, skip
                        if (text.isEmpty() && !done) {
                            return "";
                        }

                        String escapedText = text.replace("\"", "\\\"").replace("\n", "\\n");
                        return "data: {\"id\":\"" + reqId + "\",\"chunk\":\"" + escapedText + "\",\"provider\":\"" + PROVIDER_NAME + "\",\"model\":\"" + model + "\",\"done\":" + done + "}\n\n";
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse anthropic chunk", e);
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty());
    }

    private AnthropicRequest toAnthropicRequest(GenerateRequest request, String model, boolean stream) {
        // Extract system message
        String systemMessage = request.getMessages().stream()
                .filter(m -> "system".equalsIgnoreCase(m.getRole()))
                .map(com.priyansh.llmrouter.api.Message::getContent)
                .findFirst()
                .orElse(null);

        List<AnthropicRequest.Message> messages = request.getMessages().stream()
                .filter(m -> !"system".equalsIgnoreCase(m.getRole()))
                .map(m -> AnthropicRequest.Message.builder()
                        .role("user".equalsIgnoreCase(m.getRole()) ? "user" : "assistant")
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());

        return AnthropicRequest.builder()
                .model(model != null ? model : DEFAULT_MODEL)
                .max_tokens(4096)
                .system(systemMessage)
                .messages(messages)
                .stream(stream)
                .build();
    }

    private GenerateResponse toGenerateResponse(AnthropicResponse anthropicResponse, String tenantId, long startTime) {
        long latencyMs = System.currentTimeMillis() - startTime;

        String text = "";
        if (anthropicResponse.getContent() != null && !anthropicResponse.getContent().isEmpty()) {
            text = anthropicResponse.getContent().get(0).getText();
        }

        int inputTokens = 0;
        int outputTokens = 0;
        if (anthropicResponse.getUsage() != null) {
            inputTokens = anthropicResponse.getUsage().getInput_tokens();
            outputTokens = anthropicResponse.getUsage().getOutput_tokens();
        }

        return GenerateResponse.builder()
                .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                .provider(PROVIDER_NAME)
                .model(anthropicResponse.getModel())
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
        // claude-3-haiku placeholder cost
        return (inputTokens * 0.25 / 1000000.0) + (outputTokens * 1.25 / 1000000.0);
    }

    @Override
    public Mono<Boolean> checkHealth() {
        // Anthropic doesn't have a simple GET health endpoint, so we just check connectivity
        return anthropicWebClient.get()
                .uri("/")
                .retrieve()
                .toBodilessEntity()
                .map(entity -> true)
                .onErrorResume(e -> {
                    // 404 is fine (it means the service is up but the path is wrong)
                    // Connection refused/timeout is what we care about
                    log.warn("Anthropic health check failed (expected for root): {}", e.getMessage());
                    return Mono.just(!e.getMessage().contains("Connection refused") && !e.getMessage().contains("Timeout"));
                });
    }
}
