package com.priyansh.llmrouter.cache;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.api.GenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmCacheService {

    private final ReactiveRedisTemplate<String, GenerateResponse> redisTemplate;
    private final CacheKeyGenerator keyGenerator;
    private final MeterRegistry meterRegistry;

    public Mono<GenerateResponse> getCachedResponse(GenerateRequest request) {
        if (!shouldCache(request)) {
            return Mono.empty();
        }
        
        String key = keyGenerator.generateKey(request);
        return redisTemplate.opsForValue().get(key)
                .doOnNext(res -> {
                    log.info("Cache hit for key: {}", key);
                    meterRegistry.counter("llm.cache.hit").increment();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    meterRegistry.counter("llm.cache.miss").increment();
                    return Mono.empty();
                }));
    }

    public Mono<Boolean> cacheResponse(GenerateRequest request, GenerateResponse response) {
        if (!shouldCache(request)) {
            return Mono.just(false);
        }
        
        String key = keyGenerator.generateKey(request);
        Duration ttl = determineTtl(request.getTaskType());
        
        GenerateResponse cachedRes = cloneForCache(response);
        
        return redisTemplate.opsForValue().set(key, cachedRes, ttl)
                .doOnSuccess(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.info("Cached response for key: {} with TTL: {}", key, ttl);
                    }
                });
    }

    private boolean shouldCache(GenerateRequest request) {
        String taskType = request.getTaskType();
        if ("conversational".equalsIgnoreCase(taskType) || "personalized".equalsIgnoreCase(taskType)) {
            return false;
        }

        if (request.getMetadata() != null && request.getMetadata().containsKey("temperature")) {
            try {
                double temp = Double.parseDouble(String.valueOf(request.getMetadata().get("temperature")));
                if (temp > 0.5) {
                    return false;
                }
            } catch (NumberFormatException ignored) {}
        }

        return true;
    }

    private Duration determineTtl(String taskType) {
        if (taskType == null) return Duration.ofHours(1);

        switch (taskType.toLowerCase()) {
            case "faq":
            case "static_answers":
                return Duration.ofHours(24);
            case "summarization":
            case "document_summarization":
                return Duration.ofHours(3);
            case "structured_extraction":
            case "json_extraction":
                return Duration.ofHours(6);
            default:
                return Duration.ofHours(1);
        }
    }
    
    private GenerateResponse cloneForCache(GenerateResponse original) {
        return GenerateResponse.builder()
                .requestId(original.getRequestId())
                .provider(original.getProvider())
                .model(original.getModel())
                .cacheHit(true)
                .fallbackUsed(original.getFallbackUsed())
                .usage(original.getUsage())
                .output(original.getOutput())
                .build();
    }
}
