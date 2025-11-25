package com.example.reminder.controller;

import com.example.reminder.dto.ApiResponse;
import com.example.reminder.dto.EventResponse;
import com.example.reminder.dto.ReminderResponse;
import com.example.reminder.model.Event;
import com.example.reminder.security.AuthContext;
import com.example.reminder.service.EventService;
import com.example.reminder.service.ReminderService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final AuthContext authContext;
    private final ReminderService reminderService;

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getUpcomingReminders1Minute (
            @RequestParam(defaultValue = "1") long minute) {

        var currentUser = authContext.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(minute);

        List<Event> events = reminderService.getAllReminders(currentUser, now, threshold, false);

        List<ReminderResponse> reminderResponses = events.stream()
                .map(ReminderResponse::fromEntity)
                .toList();

        System.out.println("-------------------------------------");
        System.out.println(reminderResponses);

        return ResponseEntity.ok(new ApiResponse<>("success", "Upcoming reminders retrieved" ,
                reminderResponses));
    }

    @GetMapping("/upcoming/24-hours")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getUpcomingReminders24Hours () {
        var currentUser = authContext.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = LocalDateTime.now().plusHours(24);
        List<Event> events = reminderService.getAllReminders(currentUser, now, threshold , false);
        List<ReminderResponse> ReminderResponses = events.stream()
                .map(ReminderResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("success", "Upcoming next 24 hours reminders retrieved" ,
                ReminderResponses));
    }

    @GetMapping("/sent/24-hours")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getSentReminders24Hours () {
        var currentUser = authContext.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Event> events = reminderService.getAllReminders(currentUser, now, threshold, true);
        List<ReminderResponse> ReminderResponses = events.stream()
                .map(ReminderResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>("success", "Sent reminders in last 24 hours " +
                "retrieved" , ReminderResponses));
    }
}
