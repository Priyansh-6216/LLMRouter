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
public class BalancedStrategy implements RoutingStrategy {

    private final double qualityWeight = 0.35;
    private final double latencyWeight = 0.25;
    private final double costWeight = 0.20;
    private final double healthWeight = 0.20;

    @Override
    public String policyName() {
        return "balanced";
    }

    @Override
    public RoutingEngine.RoutingDecision route(GenerateRequest request, List<ModelMetadata> candidates, ProviderRegistry registry) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Policy: balanced");

        List<ScoredModel> scoredModels = candidates.stream()
                .map(m -> {
                    ProviderMetrics mtr = registry.getMetrics(m.getId());
                    double score = calculateScore(m, mtr);
                    return new ScoredModel(m, score);
                })
                .sorted(Comparator.comparingDouble(ScoredModel::score).reversed())
                .collect(Collectors.toList());

        if (scoredModels.isEmpty()) {
            throw new RuntimeException("No providers available for balanced policy");
        }

        ModelMetadata selected = scoredModels.get(0).model();
        reasons.add("Selected " + selected.getId() + " with balanced score: " + String.format("%.2f", scoredModels.get(0).score()));

        List<ModelMetadata> fallback = scoredModels.size() > 1 
                ? scoredModels.subList(1, Math.min(scoredModels.size(), 3)).stream().map(ScoredModel::model).collect(Collectors.toList())
                : new ArrayList<>();

        return RoutingEngine.RoutingDecision.builder()
                .selectedModel(selected)
                .fallbackChain(fallback)
                .reasons(reasons)
                .build();
    }

    private double calculateScore(ModelMetadata model, ProviderMetrics metrics) {
        double qualityScore = model.getQualityTier().getScore();
        
        // Normalize latency (assuming 2000ms is max "acceptable" for penalty calculation)
        double latencyPenalty = Math.min(1.0, metrics.getP95LatencyMs() / 2000.0);
        
        // Normalize cost (assuming 0.01 USD is max "acceptable" for penalty calculation)
        double cost = (model.getInputCostPer1k() + model.getOutputCostPer1k()) / 2.0;
        double costPenalty = Math.min(1.0, cost / 0.01);
        
        double healthScore = metrics.getIsHealthy() ? metrics.getSuccessRate() : 0.0;

        return (qualityWeight * qualityScore)
                - (latencyWeight * latencyPenalty)
                - (costWeight * costPenalty)
                + (healthWeight * healthScore);
    }

    private record ScoredModel(ModelMetadata model, double score) {}
}
