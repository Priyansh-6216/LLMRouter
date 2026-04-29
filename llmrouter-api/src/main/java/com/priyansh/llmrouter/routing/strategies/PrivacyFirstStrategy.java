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
public class PrivacyFirstStrategy implements RoutingStrategy {

    @Override
    public String policyName() {
        return "privacy-first";
    }

    @Override
    public RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Policy: privacy-first");

        // Prefer vLLM (self-hosted) if healthy
        List<ModelMetadata> healthyCandidates = candidates.stream()
                .filter(m -> registry.getMetrics(m.getId()).getIsHealthy())
                .sorted((m1, m2) -> {
                    boolean isVllm1 = m1.getProviderName().equalsIgnoreCase("vllm");
                    boolean isVllm2 = m2.getProviderName().equalsIgnoreCase("vllm");
                    if (isVllm1 && !isVllm2) return -1;
                    if (!isVllm1 && isVllm2) return 1;
                    return 0;
                })
                .collect(Collectors.toList());

        if (healthyCandidates.isEmpty()) {
            throw new RuntimeException("No healthy providers available for privacy-first policy");
        }

        ModelMetadata selected = healthyCandidates.get(0);
        if (selected.getProviderName().equalsIgnoreCase("vllm")) {
            reasons.add("Selected " + selected.getId() + " because it is self-hosted (vLLM)");
        } else {
            reasons.add("Selected " + selected.getId() + " (vLLM unavailable, using cloud provider)");
        }

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
