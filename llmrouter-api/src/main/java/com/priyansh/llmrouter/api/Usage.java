package com.priyansh.llmrouter.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usage {
    private Integer inputTokens;
    private Integer outputTokens;
    private Double estimatedCostUsd;
}
