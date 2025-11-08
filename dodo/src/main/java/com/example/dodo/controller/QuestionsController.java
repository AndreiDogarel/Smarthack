package com.example.dodo.controller;

import com.example.dodo.entities.Question;
import com.example.dodo.entities.QuestionDto;
import com.example.dodo.repository.QuestionsRepository;
import com.example.dodo.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionsController {
    @Autowired
    private QuestionService questionService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('PROFESOR')")
    public String addQuestion(@RequestBody QuestionDto questionDto) {
        Question question = new Question(questionDto);
        questionService.save(question);
        return question.toString();
    }


}
