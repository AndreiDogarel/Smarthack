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
}
