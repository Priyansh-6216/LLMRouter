package com.priyansh.llmrouter.routing.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderMetrics {
    private String providerName;
    private String modelName;
    private Double p95LatencyMs;
    private Double successRate; // 0.0 to 1.0
    private Boolean isHealthy;
}
