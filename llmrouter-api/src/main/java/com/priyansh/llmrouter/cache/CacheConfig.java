package com.priyansh.llmrouter.cache;

import com.priyansh.llmrouter.api.GenerateResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CacheConfig {

    @Bean
    public ReactiveRedisTemplate<String, GenerateResponse> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<GenerateResponse> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(GenerateResponse.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, GenerateResponse> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, GenerateResponse> context = 
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
