package com.priyansh.llmrouter.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "request_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAnalytics {
    @Id
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
