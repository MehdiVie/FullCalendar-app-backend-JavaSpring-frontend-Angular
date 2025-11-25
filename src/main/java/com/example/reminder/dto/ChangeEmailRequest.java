package com.example.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeEmailRequest {
    @NotBlank
    private String oldEmail;

    @NotBlank
    private String newEmail;
}
