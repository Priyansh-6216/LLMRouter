package com.priyansh.llmrouter.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Constraints {
    private Integer maxLatencyMs;
    private Double maxCostUsd;
    private String minQualityTier;
    private Boolean preferStreaming;
}
