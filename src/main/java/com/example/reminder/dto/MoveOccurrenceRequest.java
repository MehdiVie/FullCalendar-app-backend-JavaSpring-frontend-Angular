package com.example.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveOccurrenceRequest {
    @NotNull(message = "OriginalDate is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate originalDate;

    @NotNull(message = "OriginalDate is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private  LocalDate newDate;

    @NotBlank(message = "Mode is required.")
    private String mode;

}
