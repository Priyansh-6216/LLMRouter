package com.priyansh.llmrouter.providers.vllm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VllmConfig {

    @Value("${vllm.api.key:}")
    private String apiKey;

    @Value("${vllm.api.url:http://localhost:8000/v1}")
    private String apiUrl;

    @Bean
    public WebClient vllmWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        return builder.build();
    }
}
