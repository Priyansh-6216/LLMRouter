package com.priyansh.llmrouter.providers;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.api.GenerateResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProviderAdapter {
    
    /**
     * Get the unique name of the provider (e.g., "openai", "anthropic").
     */
    String providerName();

    /**
     * Execute a blocking inference request.
     */
    Mono<GenerateResponse> generate(GenerateRequest request);

    /**
     * Execute a streaming inference request.
     */
    Flux<String> generateStream(GenerateRequest request);
}
