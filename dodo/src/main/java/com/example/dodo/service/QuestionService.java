package com.example.dodo.service;

import com.example.dodo.entities.CheckAnswerDto;
import com.example.dodo.entities.Question;
import com.example.dodo.repository.QuestionsRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
