package com.example.reminder.dto;

import com.example.reminder.validation.ReminderBeforeEvent;
import com.example.reminder.validation.TomorrowOrLater;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ReminderBeforeEvent
public class EventRequest {

    @NotBlank(message = "Event Title is required.")
    private String title;

    private String description;

    @NotNull(message = "Event date is required.")
    @TomorrowOrLater
    private LocalDate eventDate;

    @FutureOrPresent(message = "Reminder time must be in the future or now.")
    private LocalDateTime reminderTime;

}
