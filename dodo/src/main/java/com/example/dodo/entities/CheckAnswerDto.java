package com.example.dodo.entities;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckAnswerDto {
    private Long questionId;
    private String answer;
}
