package com.example.reminder.dto;

import com.example.reminder.model.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReminderResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalDateTime reminderTime;

    public static ReminderResponse fromEntity(Event e) {
        return new ReminderResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getEventDate(),
                e.getReminderTime()
        );
    }
}