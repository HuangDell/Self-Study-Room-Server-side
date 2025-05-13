package com.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String studentId;
    private Integer type;
}
