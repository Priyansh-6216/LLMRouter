package com.priyansh.llmrouter.routing;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ProviderRegistry providerRegistry;

    @PostConstruct
    public void registerListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerListener);
    }

    private void registerListener(CircuitBreaker circuitBreaker) {
        String providerName = circuitBreaker.getName();
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                CircuitBreaker.StateTransition transition = event.getStateTransition();
                log.info("Circuit breaker '{}' transition: {} -> {}", 
                    providerName, transition.getFromState(), transition.getToState());
                
                if (transition.getToState() == CircuitBreaker.State.OPEN || 
                    transition.getToState() == CircuitBreaker.State.FORCED_OPEN) {
                    providerRegistry.disableProvider(providerName);
                    log.warn("Provider '{}' disabled due to open circuit breaker", providerName);
                } else if (transition.getToState() == CircuitBreaker.State.CLOSED || 
                           transition.getToState() == CircuitBreaker.State.HALF_OPEN) {
                    providerRegistry.enableProvider(providerName);
                    log.info("Provider '{}' enabled as circuit breaker is {}", providerName, transition.getToState());
                }
            });
    }
}
