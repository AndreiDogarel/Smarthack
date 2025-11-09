package com.example.dodo.controller;

import com.example.dodo.service.DocTextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
public class MaterialsController {
    private final DocTextService docTextService;
    private final RestTemplate http;
    @Value("${ai.service.url}") String aiUrl;

    public MaterialsController(DocTextService docTextService, RestTemplate http) {
        this.docTextService = docTextService;
        this.http = http;
    }

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","empty_file"));
        String text;
        try (var in = file.getInputStream()) {
            text = docTextService.extract(in);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","cannot_extract"));
        }
        var payload = new HashMap<String,Object>();
        payload.put("text", text);
        var req = new HttpEntity<>(payload);
        var res = http.postForEntity(aiUrl + "/generate-summary-quizzes", req, Map.class);
        return ResponseEntity.ok(res.getBody());
    }
}

