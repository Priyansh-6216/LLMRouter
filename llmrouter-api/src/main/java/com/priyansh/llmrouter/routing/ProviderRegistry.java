package com.priyansh.llmrouter.routing;

import com.priyansh.llmrouter.routing.model.ModelMetadata;
import com.priyansh.llmrouter.routing.model.ProviderMetrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRegistry {

    private final Map<String, ModelMetadata> models = new ConcurrentHashMap<>();
    private final Map<String, ProviderMetrics> metrics = new ConcurrentHashMap<>();

    public ProviderRegistry() {
        // Initialize with default models (Day 6 hardcoding)
        
        // OpenAI
        registerModel(ModelMetadata.builder()
                .id("openai-gpt-4o-mini")
                .providerName("openai")
                .modelName("gpt-4o-mini")
                .inputCostPer1k(0.00015)
                .outputCostPer1k(0.0006)
                .qualityTier(ModelMetadata.QualityTier.MEDIUM)
                .contextWindow(128000)
                .supportsStreaming(true)
                .build());
        
        // Anthropic
        registerModel(ModelMetadata.builder()
                .id("anthropic-claude-3-haiku")
                .providerName("anthropic")
                .modelName("claude-3-haiku-20240307")
                .inputCostPer1k(0.00025)
                .outputCostPer1k(0.00125)
                .qualityTier(ModelMetadata.QualityTier.MEDIUM)
                .contextWindow(200000)
                .supportsStreaming(true)
                .build());

        // vLLM
        registerModel(ModelMetadata.builder()
                .id("vllm-llama-3-8b")
                .providerName("vllm")
                .modelName("meta-llama/Llama-3-8B-Instruct")
                .inputCostPer1k(0.0) // Self-hosted
                .outputCostPer1k(0.0) // Self-hosted
                .qualityTier(ModelMetadata.QualityTier.MEDIUM)
                .contextWindow(8192)
                .supportsStreaming(true)
                .build());

        // Mock metrics for initial routing logic
        metrics.put("openai-gpt-4o-mini", ProviderMetrics.builder()
                .providerName("openai").modelName("gpt-4o-mini")
                .p95LatencyMs(800.0).successRate(0.99).isHealthy(true).build());
        
        metrics.put("anthropic-claude-3-haiku", ProviderMetrics.builder()
                .providerName("anthropic").modelName("claude-3-haiku-20240307")
                .p95LatencyMs(600.0).successRate(0.995).isHealthy(true).build());
        
        metrics.put("vllm-llama-3-8b", ProviderMetrics.builder()
                .providerName("vllm").modelName("meta-llama/Llama-3-8B-Instruct")
                .p95LatencyMs(300.0).successRate(0.98).isHealthy(true).build());
    }

    public void registerModel(ModelMetadata metadata) {
        models.put(metadata.getId(), metadata);
    }

    public List<ModelMetadata> getAllModels() {
        return new ArrayList<>(models.values());
    }

    public ProviderMetrics getMetrics(String modelId) {
        return metrics.getOrDefault(modelId, ProviderMetrics.builder()
                .isHealthy(true).p95LatencyMs(1000.0).successRate(1.0).build());
    }

    public void disableProvider(String providerName) {
        metrics.values().stream()
                .filter(m -> m.getProviderName().equalsIgnoreCase(providerName))
                .forEach(m -> m.setIsHealthy(false));
    }

    public void enableProvider(String providerName) {
        metrics.values().stream()
                .filter(m -> m.getProviderName().equalsIgnoreCase(providerName))
                .forEach(m -> m.setIsHealthy(true));
    }

    public void updateMetrics(String modelId, double latencyMs, boolean success) {
        ProviderMetrics current = metrics.get(modelId);
        if (current != null) {
            // Simple moving average (alpha = 0.2)
            double alpha = 0.2;
            double newLatency = (current.getP95LatencyMs() * (1 - alpha)) + (latencyMs * alpha);
            double currentSuccess = current.getSuccessRate();
            double newSuccess = (currentSuccess * (1 - alpha)) + ((success ? 1.0 : 0.0) * alpha);
            
            current.setP95LatencyMs(newLatency);
            current.setSuccessRate(newSuccess);
            log.debug("Updated metrics for {}: latency={}ms, successRate={}", modelId, newLatency, newSuccess);
        }
    }

    public List<ProviderMetrics> getAllMetrics() {
        return new ArrayList<>(metrics.values());
    }
}
