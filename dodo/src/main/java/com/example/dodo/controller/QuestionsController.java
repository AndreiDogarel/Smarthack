package com.example.dodo.controller;

import com.example.dodo.entities.CheckAnswerDto;
import com.example.dodo.entities.Question;
import com.example.dodo.entities.QuestionDto;
import com.example.dodo.repository.QuestionsRepository;
import com.example.dodo.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "http://localhost:4200")
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

    @GetMapping("/checkAnswer")
    @PreAuthorize("hasRole('STUDENT')")
    public Boolean checkAnswer(@RequestBody CheckAnswerDto answer) {
        return questionService.checkAnswer(answer);

    }

    @GetMapping("/getQuestionsByDomain/{domain}")
//    @PreAuthorize("hasRole('STUDENT')")
    public List<QuestionDto> getQuestionsByDomain(@PathVariable("domain") String domain) {
        return questionService.getQuestionsByDomain(domain);
    }


}
