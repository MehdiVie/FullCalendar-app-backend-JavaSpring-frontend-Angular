package com.example.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResult {
    @NotBlank
    private String email;

    private boolean expired;


}
