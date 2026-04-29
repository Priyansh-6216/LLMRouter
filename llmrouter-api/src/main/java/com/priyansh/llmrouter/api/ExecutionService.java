package com.priyansh.llmrouter.api;

import com.priyansh.llmrouter.providers.ProviderAdapter;
import com.priyansh.llmrouter.routing.RoutingEngine;
import com.priyansh.llmrouter.routing.model.ModelMetadata;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final List<ProviderAdapter> adapters;
    private final com.priyansh.llmrouter.routing.ProviderRegistry providerRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public Mono<GenerateResponse> executeWithFallback(GenerateRequest request, RoutingEngine.RoutingDecision decision) {
        return tryProvider(request, decision.getSelectedModel(), decision.getFallbackChain(), 0);
    }

    private Mono<GenerateResponse> tryProvider(GenerateRequest request, ModelMetadata currentModel, List<ModelMetadata> fallbackChain, int fallbackIndex) {
        String provider = currentModel.getProviderName();
        ProviderAdapter adapter = getAdapter(provider);
        long startTime = System.currentTimeMillis();

        return adapter.generate(request)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(provider)))
                .transformDeferred(RetryOperator.of(retryRegistry.retry(provider)))
                .doOnSuccess(res -> {
                    long latency = System.currentTimeMillis() - startTime;
                    providerRegistry.updateMetrics(currentModel.getId(), latency, true);
                })
                .doOnError(e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    providerRegistry.updateMetrics(currentModel.getId(), latency, false);
                    log.warn("Provider {} failed for model {}: {}", provider, currentModel.getModelName(), e.getMessage());
                })
                .onErrorResume(e -> {
                    if (fallbackIndex < fallbackChain.size()) {
                        ModelMetadata nextModel = fallbackChain.get(fallbackIndex);
                        log.info("Falling back to provider: {}", nextModel.getProviderName());
                        return tryProvider(request, nextModel, fallbackChain, fallbackIndex + 1)
                                .map(res -> {
                                    res.setFallbackUsed(true);
                                    return res;
                                });
                    }
                    return Mono.error(new RuntimeException("All fallback providers exhausted", e));
                });
    }

    public Flux<String> executeStreamWithFallback(GenerateRequest request, RoutingEngine.RoutingDecision decision) {
        return tryProviderStream(request, decision.getSelectedModel(), decision.getFallbackChain(), 0);
    }

    private Flux<String> tryProviderStream(GenerateRequest request, ModelMetadata currentModel, List<ModelMetadata> fallbackChain, int fallbackIndex) {
        String provider = currentModel.getProviderName();
        ProviderAdapter adapter = getAdapter(provider);
        long startTime = System.currentTimeMillis();

        return adapter.generateStream(request)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(provider)))
                .doOnComplete(() -> {
                    long latency = System.currentTimeMillis() - startTime;
                    providerRegistry.updateMetrics(currentModel.getId(), latency, true);
                })
                .doOnError(e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    providerRegistry.updateMetrics(currentModel.getId(), latency, false);
                    log.warn("Stream Provider {} failed: {}", provider, e.getMessage());
                })
                .onErrorResume(e -> {
                    if (fallbackIndex < fallbackChain.size()) {
                        ModelMetadata nextModel = fallbackChain.get(fallbackIndex);
                        log.info("Falling back stream to provider: {}", nextModel.getProviderName());
                        return tryProviderStream(request, nextModel, fallbackChain, fallbackIndex + 1);
                    }
                    return Flux.error(new RuntimeException("All fallback providers exhausted for stream", e));
                });
    }

    private ProviderAdapter getAdapter(String provider) {
        return adapters.stream()
                .filter(a -> a.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider adapter not found: " + provider));
    }
}
