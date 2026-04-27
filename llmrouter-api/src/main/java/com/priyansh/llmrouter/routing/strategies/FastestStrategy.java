package com.priyansh.llmrouter.routing.strategies;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.routing.ProviderRegistry;
import com.priyansh.llmrouter.routing.RoutingEngine;
import com.priyansh.llmrouter.routing.RoutingStrategy;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FastestStrategy implements RoutingStrategy {

    @Override
    public String policyName() {
        return "fastest";
    }

    @Override
    public RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Policy: fastest");

        List<ModelMetadata> healthyCandidates = candidates.stream()
                .filter(m -> registry.getMetrics(m.getId()).getIsHealthy())
                .sorted(Comparator.comparingDouble(m -> registry.getMetrics(m.getId()).getP95LatencyMs()))
                .collect(Collectors.toList());

        if (healthyCandidates.isEmpty()) {
            throw new RuntimeException("No healthy providers available for fastest policy");
        }

        ModelMetadata selected = healthyCandidates.get(0);
        reasons.add("Selected " + selected.getId() + " with p95 latency: " + registry.getMetrics(selected.getId()).getP95LatencyMs() + "ms");

        List<ModelMetadata> fallback = healthyCandidates.size() > 1 
                ? healthyCandidates.subList(1, Math.min(healthyCandidates.size(), 3)) 
                : new ArrayList<>();

        return RoutingEngine.RoutingDecision.builder()
                .selectedModel(selected)
                .fallbackChain(fallback)
                .reasons(reasons)
                .build();
    }
}
