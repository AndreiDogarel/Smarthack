package com.example.dodo.service;

import com.example.dodo.entities.CheckAnswerDto;
import com.example.dodo.entities.Question;
import com.example.dodo.entities.QuestionDto;
import com.example.dodo.repository.QuestionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    @Autowired
    private QuestionsRepository questionsRepository;

    public void save(Question question) {
        questionsRepository.save(question);
    }

    public Boolean checkAnswer(CheckAnswerDto answer) {
        String correctAnswer = questionsRepository.getCorrectAnswerByQuestionId(answer.getQuestionId());
        return correctAnswer.equals(answer.getAnswer());
    }

    public List<QuestionDto> getQuestionsByDomain(String domain) {
        return questionsRepository.getQuestionsByDomain(domain).stream().map(QuestionDto::new).collect(Collectors.toList());
    }
}
