package com.priyansh.llmrouter.routing;

import com.priyansh.llmrouter.providers.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final List<ProviderAdapter> adapters;
    private final ProviderRegistry providerRegistry;

    @Scheduled(fixedRateString = "${app.healthcheck.interval:30000}")
    public void performHealthChecks() {
        log.debug("Performing proactive health checks for providers...");
        
        Flux.fromIterable(adapters)
                .flatMap(adapter -> adapter.checkHealth()
                        .doOnNext(healthy -> {
                            if (healthy) {
                                providerRegistry.enableProvider(adapter.providerName());
                            } else {
                                providerRegistry.disableProvider(adapter.providerName());
                                log.warn("Proactive health check failed for provider: {}", adapter.providerName());
                            }
                        })
                        .onErrorResume(e -> {
                            providerRegistry.disableProvider(adapter.providerName());
                            return Flux.empty();
                        }))
                .subscribe();
    }
}
