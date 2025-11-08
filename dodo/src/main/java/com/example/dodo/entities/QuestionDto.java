package com.example.dodo.entities;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {
    private String question;
    private String variantA;
    private String variantB;
    private String variantC;
    private String variantD;
    private String correctAnswer;
    private String domain;

    public QuestionDto(Question question) {
        this.question = question.getQuestion();
        this.variantA = question.getVariantA();
        this.variantB = question.getVariantB();
        this.variantC = question.getVariantC();
        this.variantD = question.getVariantD();
        this.correctAnswer = question.getCorrectAnswer();
        this.domain = question.getDomain();
    }
}
