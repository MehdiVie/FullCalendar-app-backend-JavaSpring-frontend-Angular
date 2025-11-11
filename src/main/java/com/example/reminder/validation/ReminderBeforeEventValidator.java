package com.example.reminder.validation;

import com.example.reminder.dto.EventRequest;
import com.example.reminder.model.Event;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReminderBeforeEventValidator implements ConstraintValidator<ReminderBeforeEvent, EventRequest> {

    @Override
    public boolean isValid(EventRequest dto , ConstraintValidatorContext context) {
        if (dto == null || dto.getReminderTime()==null || dto.getEventDate()==null)
            return true;
        LocalDate eventDate = dto.getEventDate();
        LocalDateTime reminder = dto.getReminderTime();

        LocalDate reminderDateOnly = reminder.toLocalDate();

        boolean valid= reminderDateOnly.isBefore(eventDate);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "ReminderTime must be before the EventDate."
            ).addPropertyNode("reminderTime").addConstraintViolation();
        }

        return  valid;
    }
}
