package com.priyansh.llmrouter.analytics;

import com.priyansh.llmrouter.analytics.entity.RequestAnalytics;
import com.priyansh.llmrouter.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsRepository repository;

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsage(@RequestParam String tenantId) {
        List<RequestAnalytics> logs = repository.findByTenantId(tenantId);
        
        long totalRequests = logs.size();
        int totalInputTokens = logs.stream().mapToInt(l -> l.getInputTokens() != null ? l.getInputTokens() : 0).sum();
        int totalOutputTokens = logs.stream().mapToInt(l -> l.getOutputTokens() != null ? l.getOutputTokens() : 0).sum();
        double totalCost = logs.stream().mapToDouble(l -> l.getEstimatedCostUsd() != null ? l.getEstimatedCostUsd() : 0.0).sum();
        long cacheHits = logs.stream().filter(l -> Boolean.TRUE.equals(l.getCacheHit())).count();
        long fallbacks = logs.stream().filter(l -> Boolean.TRUE.equals(l.getFallbackUsed())).count();

        Map<String, Object> stats = Map.of(
            "tenantId", tenantId,
            "totalRequests", totalRequests,
            "totalInputTokens", totalInputTokens,
            "totalOutputTokens", totalOutputTokens,
            "totalCostUsd", totalCost,
            "cacheHitRate", totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0,
            "fallbackRate", totalRequests > 0 ? (double) fallbacks / totalRequests : 0.0
        );

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RequestAnalytics>> getRecentLogs(@RequestParam String tenantId) {
        return ResponseEntity.ok(repository.findByTenantId(tenantId));
    }
}
