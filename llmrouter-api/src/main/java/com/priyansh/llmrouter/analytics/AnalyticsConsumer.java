package com.priyansh.llmrouter.analytics;

import com.priyansh.llmrouter.analytics.event.RequestEvent;
import com.priyansh.llmrouter.analytics.entity.RequestAnalytics;
import com.priyansh.llmrouter.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsRepository repository;

    @KafkaListener(topics = "llm.requests", groupId = "llmrouter-analytics-group")
    public void consumeRequestEvent(RequestEvent event) {
        log.info("Consuming request event for ID: {}", event.getRequestId());
        
        RequestAnalytics analytics = RequestAnalytics.builder()
                .requestId(event.getRequestId())
                .tenantId(event.getTenantId())
                .provider(event.getProvider())
                .model(event.getModel())
                .latencyMs(event.getLatencyMs())
                .inputTokens(event.getInputTokens())
                .outputTokens(event.getOutputTokens())
                .estimatedCostUsd(event.getEstimatedCostUsd())
                .cacheHit(event.getCacheHit())
                .fallbackUsed(event.getFallbackUsed())
                .taskType(event.getTaskType())
                .routingPolicy(event.getRoutingPolicy())
                .timestamp(event.getTimestamp())
                .error(event.getError())
                .build();
        
        repository.save(analytics);
    }
}
