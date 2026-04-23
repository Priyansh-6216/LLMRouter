package com.priyansh.llmrouter.providers.anthropic;

import lombok.Data;

import java.util.List;

@Data
public class AnthropicResponse {
    private String id;
    private String type;
    private String role;
    private String model;
    private List<Content> content;
    private Usage usage;

    @Data
    public static class Content {
        private String type;
        private String text;
    }

    @Data
    public static class Usage {
        private int input_tokens;
        private int output_tokens;
    }
}
