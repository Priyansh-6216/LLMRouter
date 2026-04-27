package com.priyansh.llmrouter.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetadata {
    private String id;
    private String providerName;
    private String modelName;
    private Double inputCostPer1k;
    private Double outputCostPer1k;
    private QualityTier qualityTier;
    private Integer contextWindow;
    private Boolean supportsStreaming;
    
    public enum QualityTier {
        LOW(0.25), MEDIUM(0.50), HIGH(0.75), PREMIUM(1.0);
        
        private final double score;
        
        QualityTier(double score) {
            this.score = score;
        }
        
        public double getScore() {
            return score;
        }
    }
}
