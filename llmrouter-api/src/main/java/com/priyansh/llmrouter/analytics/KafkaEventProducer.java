package com.priyansh.llmrouter.analytics;

import com.priyansh.llmrouter.analytics.event.RequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_REQUESTS = "llm.requests";

    public void sendRequestEvent(RequestEvent event) {
        log.debug("Sending request event to Kafka: {}", event.getRequestId());
        kafkaTemplate.send(TOPIC_REQUESTS, event.getRequestId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send Kafka event for request {}", event.getRequestId(), ex);
                    } else {
                        log.trace("Successfully sent Kafka event for request {}", event.getRequestId());
                    }
                });
    }
}
