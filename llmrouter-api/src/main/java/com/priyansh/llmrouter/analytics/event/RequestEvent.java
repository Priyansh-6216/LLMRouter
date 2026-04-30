package com.priyansh.llmrouter.analytics.event;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class RequestEvent {
    private String requestId;
    private String tenantId;
    private String provider;
    private String model;
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double estimatedCostUsd;
    private Boolean cacheHit;
    private Boolean fallbackUsed;
    private String taskType;
    private String routingPolicy;
    private Instant timestamp;
    private String error;
}
