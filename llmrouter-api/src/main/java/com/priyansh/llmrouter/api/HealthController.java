package com.priyansh.llmrouter.api;

import com.priyansh.llmrouter.routing.ProviderRegistry;
import com.priyansh.llmrouter.routing.model.ProviderMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class HealthController {

    private final ProviderRegistry providerRegistry;

    @GetMapping("/providers/health")
    public ResponseEntity<List<ProviderMetrics>> getProviderHealth() {
        return ResponseEntity.ok(providerRegistry.getAllMetrics());
    }

    @PostMapping("/admin/providers/{provider}/disable")
    public ResponseEntity<String> disableProvider(@PathVariable String provider) {
        providerRegistry.disableProvider(provider);
        return ResponseEntity.ok("Provider " + provider + " disabled successfully.");
    }
    
    @PostMapping("/admin/providers/{provider}/enable")
    public ResponseEntity<String> enableProvider(@PathVariable String provider) {
        providerRegistry.enableProvider(provider);
        return ResponseEntity.ok("Provider " + provider + " enabled successfully.");
    }
}
