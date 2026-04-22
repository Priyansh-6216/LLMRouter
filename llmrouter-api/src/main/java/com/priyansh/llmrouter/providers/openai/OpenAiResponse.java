package com.priyansh.llmrouter.providers.openai;

import lombok.Data;

import java.util.List;

@Data
public class OpenAiResponse {
    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private Delta delta;
        private String finish_reason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
    
    @Data
    public static class Delta {
        private String content;
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}
