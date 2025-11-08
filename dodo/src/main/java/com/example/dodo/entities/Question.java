package com.example.dodo.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Data
@AllArgsConstructor
@Builder
@Table(name = "QUESTIONS")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "questions_seq")
    @SequenceGenerator(name = "questions_seq", sequenceName = "questions_seq", allocationSize = 1)
    private Long id;

    @Column(name = "question_text")
    private String question;

    @Column(name = "variant_a")
    private String variantA;

    @Column(name = "variant_b")
    private String variantB;

    @Column(name = "variant_c")
    private String variantC;

    @Column(name = "variant_d")
    private String variantD;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "domeniu")
    private String domain;

    public Question(String question, String variantA, String variantB, String variantC, String variantD, String correctAnswer,  String domain) {
        this.question = question;
        this.variantA = variantA;
        this.variantB = variantB;
        this.variantC = variantC;
        this.variantD = variantD;
        this.correctAnswer = correctAnswer;
        this.domain = domain;
    }

    public Question() {

    }
    public Question(QuestionDto questionDto) {
        this.question = questionDto.getQuestion();
        this.variantA = questionDto.getVariantA();
        this.variantB = questionDto.getVariantB();
        this.variantC = questionDto.getVariantC();
        this.variantD = questionDto.getVariantD();
        this.correctAnswer = questionDto.getCorrectAnswer();
        this.domain = questionDto.getDomain();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
