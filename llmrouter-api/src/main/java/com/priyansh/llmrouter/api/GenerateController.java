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

    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerateResponse>> generate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GenerateRequest request) {
        
        // For Day 3, we just find the OpenAI adapter (or any available) and use it.
        // Later, the Routing Engine will determine which adapter to use based on policies.
        ProviderAdapter adapter = adapters.stream()
                .filter(a -> a.providerName().equals("openai"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OpenAI adapter not found"));

        return adapter.generate(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GenerateRequest request) {

        ProviderAdapter adapter = adapters.stream()
                .filter(a -> a.providerName().equals("openai"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OpenAI adapter not found"));

        return adapter.generateStream(request);
    }
}
