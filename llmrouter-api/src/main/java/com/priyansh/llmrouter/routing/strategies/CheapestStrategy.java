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
public class CheapestStrategy implements RoutingStrategy {

    @Override
    public String policyName() {
        return "cheapest";
    }

    @Override
    public RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Policy: cheapest");

        // Filter by health
        List<ModelMetadata> healthyCandidates = candidates.stream()
                .filter(m -> registry.getMetrics(m.getId()).getIsHealthy())
                .collect(Collectors.toList());

        if (healthyCandidates.isEmpty()) {
            throw new RuntimeException("No healthy providers available for cheapest policy");
        }

        // Sort by cost (input + output average)
        healthyCandidates.sort(Comparator.comparingDouble(m -> (m.getInputCostPer1k() + m.getOutputCostPer1k())));

        ModelMetadata selected = healthyCandidates.get(0);
        reasons.add("Selected " + selected.getId() + " as the lowest cost option");

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
