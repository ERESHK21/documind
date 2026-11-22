package com.documind.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final boolean aiConfigured;
    private final String aiModel;

    public MetaController(
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gemini-2.5-flash}") String aiModel,
            @Value("${app.ai.provider:auto}") String provider) {
        this.aiConfigured = !apiKey.isBlank() && !"offline".equalsIgnoreCase(provider);
        this.aiModel = aiModel;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "ok");
        map.put("service", "documind-backend");
        map.put("time", Instant.now().toString());
        map.put("aiConfigured", aiConfigured);
        map.put("aiModel", aiConfigured ? aiModel : "offline-extractive");
        return map;
    }
}
