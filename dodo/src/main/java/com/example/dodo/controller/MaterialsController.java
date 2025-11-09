package com.example.dodo.controller;

import com.example.dodo.entities.UploadResultDto;
import com.example.dodo.service.DocTextService;
import com.example.dodo.service.UploadIngestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
public class MaterialsController {
    private final DocTextService docTextService;
    private final RestTemplate http;
    private final UploadIngestService ingestService;

    @Value("${ai.service.url}") String aiUrl;

    public MaterialsController(DocTextService docTextService, RestTemplate http, UploadIngestService ingestService) {
        this.docTextService = docTextService;
        this.http = http;
        this.ingestService = ingestService;
    }

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value="domain", required=false, defaultValue="general") String domain) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","empty_file"));
        String text;
        try (var in = file.getInputStream()) { text = docTextService.extract(in); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error","cannot_extract")); }

        var req = new HttpEntity<>(Map.of("text", text));
        var aiRes = http.postForEntity(aiUrl + "/generate-summary-quizzes", req, UploadResultDto.class);
        var dto = aiRes.getBody();
        if (dto == null) return ResponseEntity.badRequest().body(Map.of("error","ai_empty_response"));

        int saved = ingestService.saveQuestions(dto, domain);

        var ui = new HashMap<String,Object>();
        ui.put("filename", file.getOriginalFilename());
        ui.put("domain", domain);
        ui.put("summary", dto.getSummary());
        ui.put("saved_questions", saved);
        ui.put("quizzes", dto.getQuizzes().stream().map(q -> Map.of(
                "question", q.getQuestion(),
                "options", q.getOptions(),
                "answerIndex", q.getAnswer_index()
        )).toList());
        return ResponseEntity.ok(ui);
    }
}
