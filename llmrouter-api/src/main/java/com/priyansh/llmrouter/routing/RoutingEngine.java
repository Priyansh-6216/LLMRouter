package com.priyansh.llmrouter.routing;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import lombok.Builder;
import lombok.Data;
import java.util.List;

public interface RoutingEngine {
    
    RoutingDecision selectRoute(GenerateRequest request);

    @Data
    @Builder
    class RoutingDecision {
        private ModelMetadata selectedModel;
        private List<ModelMetadata> fallbackChain;
        private List<String> reasons;
    }
}
