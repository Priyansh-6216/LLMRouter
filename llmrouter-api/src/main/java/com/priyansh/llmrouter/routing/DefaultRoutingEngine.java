package com.priyansh.llmrouter.routing;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRoutingEngine implements RoutingEngine {

    private final ProviderRegistry registry;
    private final List<RoutingStrategy> strategies;

    @Override
    public RoutingDecision selectRoute(GenerateRequest request) {
        String policyName = request.getRoutingPolicy() != null ? request.getRoutingPolicy() : "balanced";
        
        RoutingStrategy strategy = strategies.stream()
                .filter(s -> s.policyName().equalsIgnoreCase(policyName))
                .findFirst()
                .orElse(strategies.stream()
                        .filter(s -> s.policyName().equalsIgnoreCase("balanced"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No routing strategies found")));

        log.info("Routing request for tenant {} using policy {}", request.getTenantId(), policyName);

        List<ModelMetadata> candidates = registry.getAllModels();
        
        // Apply basic constraint filtering (e.g., context window)
        // For Day 6, we keep it simple
        
        return strategy.route(request, candidates, registry);
    }
}
