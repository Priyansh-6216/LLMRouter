package com.priyansh.llmrouter.routing.strategies;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.routing.ProviderRegistry;
import com.priyansh.llmrouter.routing.RoutingEngine;
import com.priyansh.llmrouter.routing.RoutingStrategy;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import com.priyansh.llmrouter.routing.model.ProviderMetrics;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResilientStrategy implements RoutingStrategy {

    @Override
    public String policyName() {
        return "resilient";
    }

    @Override
    public RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Policy: resilient");

        // Filter by health and sort by success rate
        List<ModelMetadata> healthyCandidates = candidates.stream()
                .filter(m -> registry.getMetrics(m.getId()).getIsHealthy())
                .sorted(Comparator.comparingDouble((ModelMetadata m) -> registry.getMetrics(m.getId()).getSuccessRate()).reversed())
                .collect(Collectors.toList());

        if (healthyCandidates.isEmpty()) {
            // If none are "healthy", pick any that isn't disabled manually (though registry.getIsHealthy usually covers circuit breaker too now)
            throw new RuntimeException("No providers available for resilient policy");
        }

        ModelMetadata selected = healthyCandidates.get(0);
        ProviderMetrics metrics = registry.getMetrics(selected.getId());
        reasons.add("Selected " + selected.getId() + " with success rate: " + (metrics.getSuccessRate() * 100) + "%");

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
