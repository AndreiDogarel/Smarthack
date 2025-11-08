package com.example.dodo.entities;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserLoginDto {
    private String username;
    private String password;
}