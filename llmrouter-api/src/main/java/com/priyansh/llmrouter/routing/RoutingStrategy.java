package com.priyansh.llmrouter.routing;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import com.priyansh.llmrouter.routing.model.ProviderMetrics;

import java.util.List;

public interface RoutingStrategy {
    String policyName();
    RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry);
}
