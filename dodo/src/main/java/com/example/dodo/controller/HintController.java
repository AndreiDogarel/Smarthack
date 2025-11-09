package com.example.dodo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class HintController {
    private final RestTemplate http;
    @Value("${ai.service.url}") String aiUrl;

    public HintController(RestTemplate http) { this.http = http; }

    @PostMapping("/hint")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> hint(@RequestBody Map<String,String> body){
        if (body == null || body.getOrDefault("question","").isBlank())
            return ResponseEntity.badRequest().body(Map.of("error","missing_question"));
        var res = http.postForEntity(aiUrl + "/hint", body, Map.class);
        return ResponseEntity.ok(res.getBody());
    }
}

