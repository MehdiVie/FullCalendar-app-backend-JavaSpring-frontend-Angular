package com.example.reminder.service;

import com.example.reminder.model.Event;
import com.example.reminder.model.User;
import com.example.reminder.repository.EventRepository;
import com.example.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final EventRepository repo;
    public List<Event> getAllReminders(User user, LocalDateTime now, LocalDateTime threshold ,
                                       boolean sent) {

        if (!sent) return repo.findAllUpcomingReminders(user, now, threshold);

        return repo.findAllSentReminders(user, now, threshold);

    }
}
