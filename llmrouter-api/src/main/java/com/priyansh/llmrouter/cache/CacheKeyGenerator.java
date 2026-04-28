package com.priyansh.llmrouter.cache;

import com.priyansh.llmrouter.api.GenerateRequest;
import com.priyansh.llmrouter.api.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class CacheKeyGenerator {

    public String generateKey(GenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        
        // normalize(systemPrompt) + normalize(userContent)
        if (request.getMessages() != null) {
            for (Message msg : request.getMessages()) {
                sb.append(msg.getRole() != null ? msg.getRole().toLowerCase() : "").append(":");
                if (msg.getContent() != null) {
                    sb.append(msg.getContent().trim().toLowerCase());
                }
                sb.append("|");
            }
        }

        // taskType
        sb.append(request.getTaskType()).append("|");
        
        // temperatureBucket - rounded to nearest 0.1
        if (request.getMetadata() != null && request.getMetadata().containsKey("temperature")) {
            try {
                double temp = Double.parseDouble(String.valueOf(request.getMetadata().get("temperature")));
                double bucket = Math.round(temp * 10.0) / 10.0;
                sb.append(bucket).append("|");
            } catch (NumberFormatException e) {
                sb.append("default|");
            }
        } else {
            sb.append("default|");
        }

        // outputSchema
        if (request.getMetadata() != null && request.getMetadata().containsKey("outputSchema")) {
            sb.append(request.getMetadata().get("outputSchema")).append("|");
        }

        // policyVersion
        sb.append(request.getRoutingPolicy());

        return "llm:cache:" + hash(sb.toString());
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
