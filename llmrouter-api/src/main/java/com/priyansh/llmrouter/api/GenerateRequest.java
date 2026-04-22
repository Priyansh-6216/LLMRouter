package com.priyansh.llmrouter.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {
    private String tenantId;
    private String taskType;
    private List<Message> messages;
    private Constraints constraints;
    private String routingPolicy;
    private Map<String, String> metadata;
}
