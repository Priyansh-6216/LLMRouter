package com.priyansh.llmrouter.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class GenerateController {

    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerateResponse>> generate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GenerateRequest request) {
        
        // STUB implementation for Day 2
        GenerateResponse response = GenerateResponse.builder()
                .requestId("req_" + UUID.randomUUID().toString().substring(0, 8))
                .provider("stub-provider")
                .model("stub-model")
                .latencyMs(150L)
                .cacheHit(false)
                .fallbackUsed(false)
                .usage(Usage.builder()
                        .inputTokens(request.getMessages().size() * 10)
                        .outputTokens(50)
                        .estimatedCostUsd(0.001)
                        .build())
                .output(Map.of("text", "This is a stub response for Day 2!"))
                .build();

        return Mono.just(ResponseEntity.ok(response));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @RequestHeader("Authorization") String authorization,
            @RequestBody GenerateRequest request) {

        // STUB streaming implementation for Day 2
        String reqId = "req_" + UUID.randomUUID().toString().substring(0, 8);
        return Flux.just(
                "data: {\"id\":\"" + reqId + "\",\"chunk\":\"This \",\"provider\":\"stub-provider\",\"model\":\"stub-model\",\"done\":false}\n\n",
                "data: {\"id\":\"" + reqId + "\",\"chunk\":\"is a \",\"provider\":\"stub-provider\",\"model\":\"stub-model\",\"done\":false}\n\n",
                "data: {\"id\":\"" + reqId + "\",\"chunk\":\"stub stream!\",\"provider\":\"stub-provider\",\"model\":\"stub-model\",\"done\":true,\"usage\":{\"inputTokens\":10,\"outputTokens\":5,\"estimatedCostUsd\":0.001}}\n\n"
        );
    }
}
