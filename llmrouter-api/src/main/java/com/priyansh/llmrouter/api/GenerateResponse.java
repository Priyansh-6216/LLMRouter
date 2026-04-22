package com.priyansh.llmrouter.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {
    private String requestId;
    private String provider;
    private String model;
    private Long latencyMs;
    private Boolean cacheHit;
    private Boolean fallbackUsed;
    private Usage usage;
    private Map<String, Object> output;
}
