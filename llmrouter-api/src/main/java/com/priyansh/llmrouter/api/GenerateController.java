package com.priyansh.llmrouter.api;

import com.priyansh.llmrouter.providers.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class GenerateController {

    private final com.priyansh.llmrouter.routing.RoutingEngine routingEngine;
    private final com.priyansh.llmrouter.cache.LlmCacheService cacheService;
    private final ExecutionService executionService;
    private final com.priyansh.llmrouter.analytics.KafkaEventProducer eventProducer;

    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerateResponse>> generate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GenerateRequest request) {
        
        return cacheService.getCachedResponse(request)
                .map(res -> {
                    publishEvent(request, res, true, null);
                    return ResponseEntity.ok(res);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    com.priyansh.llmrouter.routing.RoutingEngine.RoutingDecision decision = routingEngine.selectRoute(request);
                    
                    return executionService.executeWithFallback(request, decision)
                            .flatMap(res -> cacheService.cacheResponse(request, res).thenReturn(res))
                            .map(res -> {
                                publishEvent(request, res, false, decision.getRoutingPolicy());
                                return ResponseEntity.ok(res);
                            })
                            .onErrorResume(e -> {
                                publishErrorEvent(request, e, decision.getRoutingPolicy());
                                return Mono.error(e);
                            });
                }));
    }

    private void publishEvent(GenerateRequest request, GenerateResponse response, boolean cacheHit, String policy) {
        com.priyansh.llmrouter.analytics.event.RequestEvent event = com.priyansh.llmrouter.analytics.event.RequestEvent.builder()
                .requestId(response.getRequestId())
                .tenantId(request.getTenantId())
                .provider(response.getProvider())
                .model(response.getModel())
                .latencyMs(response.getLatencyMs())
                .inputTokens(response.getUsage() != null ? response.getUsage().getInputTokens() : 0)
                .outputTokens(response.getUsage() != null ? response.getUsage().getOutputTokens() : 0)
                .estimatedCostUsd(response.getUsage() != null ? response.getUsage().getEstimatedCostUsd() : 0.0)
                .cacheHit(cacheHit)
                .fallbackUsed(response.getFallbackUsed())
                .taskType(request.getTaskType())
                .routingPolicy(policy != null ? policy : request.getRoutingPolicy())
                .timestamp(java.time.Instant.now())
                .build();
        eventProducer.sendRequestEvent(event);
    }

    private void publishErrorEvent(GenerateRequest request, Throwable e, String policy) {
        com.priyansh.llmrouter.analytics.event.RequestEvent event = com.priyansh.llmrouter.analytics.event.RequestEvent.builder()
                .requestId("err_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .tenantId(request.getTenantId())
                .taskType(request.getTaskType())
                .routingPolicy(policy != null ? policy : request.getRoutingPolicy())
                .timestamp(java.time.Instant.now())
                .error(e.getMessage())
                .build();
        eventProducer.sendRequestEvent(event);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GenerateRequest request) {

        com.priyansh.llmrouter.routing.RoutingEngine.RoutingDecision decision = routingEngine.selectRoute(request);
        return executionService.executeStreamWithFallback(request, decision);
    }
}
