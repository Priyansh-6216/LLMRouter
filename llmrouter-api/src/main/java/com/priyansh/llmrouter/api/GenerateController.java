package com.priyansh.llmrouter.api;

import com.priyansh.llmrouter.providers.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class GenerateController {

    private final List<ProviderAdapter> adapters;
    private final com.priyansh.llmrouter.routing.RoutingEngine routingEngine;

    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerateResponse>> generate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GenerateRequest request) {
        
        com.priyansh.llmrouter.routing.RoutingEngine.RoutingDecision decision = routingEngine.selectRoute(request);
        String provider = decision.getSelectedModel().getProviderName();

        ProviderAdapter adapter = adapters.stream()
                .filter(a -> a.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider adapter not found: " + provider));

        return adapter.generate(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody GenerateRequest request) {

        com.priyansh.llmrouter.routing.RoutingEngine.RoutingDecision decision = routingEngine.selectRoute(request);
        String provider = decision.getSelectedModel().getProviderName();

        ProviderAdapter adapter = adapters.stream()
                .filter(a -> a.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider adapter not found: " + provider));

        return adapter.generateStream(request);
    }
}
