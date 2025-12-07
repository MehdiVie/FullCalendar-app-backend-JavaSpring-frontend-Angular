package com.example.reminder.service;
import com.example.reminder.dto.EventRequest;
import com.example.reminder.dto.EventResponse;
import com.example.reminder.dto.MoveOccurrenceRequest;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.model.Event;
import com.example.reminder.model.RecurrenceType;
import com.example.reminder.model.User;
import com.example.reminder.repository.EventRepository;
import com.example.reminder.security.AuthContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
public class EventService {
    private final EventRepository repo;
    private final EmailService emailService;



    // Allowed sort fields (white list)
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "eventDate", "title", "reminderTime");


    public EventService(EventRepository repository, EmailService emailService) {
        this.repo = repository;
        this.emailService = emailService;
    }


    public  Page<Event> getPagedEventsForUser(User user,Integer page, Integer size, String sortBy,
                                      String direction, LocalDate afterDate, String search) {
        // defaults
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 10 : size;

        // safe direction
        Sort.Direction dir;
        try {
            dir = (direction == null) ? Sort.Direction.ASC : Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            dir = Sort.Direction.ASC;
        }

        //safe sortBy
        String sortProb = (sortBy == null || !ALLOWED_SORTS.contains(sortBy)) ? "id" : sortBy;

        // stable sort
        Sort sort = Sort.by(new Sort.Order(dir, sortProb) , new Sort.Order(dir , "id"));

        Pageable pageable = PageRequest.of(p,s,sort);

        if(afterDate != null && search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findByUserAndAfterDateAndSearch(user, afterDate, likeSearch, pageable);
        } else if (afterDate != null) {
            return repo.findByUserAndAfterDate(user,afterDate, pageable);
        } else if (search != null && !search.isEmpty()){
            String likeSearch = "%" + search.toLowerCase().trim() + "%";

            return repo.findByUserAndSearch(user, likeSearch, pageable);
        }


        return repo.findByUser(user,pageable);


    }

    public  Page<Event> getPagedEventsForAdmin(Integer page, Integer size, String sortBy,
                                              String direction, LocalDate afterDate, String search) {
        // defaults
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 10 : size;

        // safe direction
        Sort.Direction dir;
        try {
            dir = (direction == null) ? Sort.Direction.ASC : Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            dir = Sort.Direction.ASC;
        }

        //safe sortBy
        String sortProb = (sortBy == null || !ALLOWED_SORTS.contains(sortBy)) ? "id" : sortBy;

        // stable sort
        Sort sort = Sort.by(new Sort.Order(dir, sortProb) , new Sort.Order(dir , "id"));

        Pageable pageable = PageRequest.of(p,s,sort);

        if (afterDate != null && search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findAllEventsAndAfterDateAndSearch(afterDate, likeSearch ,pageable);
        } else if (search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findAllEventsAndSearch(likeSearch ,pageable);
        } else if (afterDate != null) {

            return repo.findAllEventsAndAfterDate(afterDate ,pageable);
        }


        return repo.findAll(pageable);

    }

    public List<Map<String,Object>> getEventsPerDay() {

        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(29);

        List<Object[]> raw = repo.eventsPerDaySince(from,to);

        List<Map<String,Object>> result = new ArrayList<>();

        for(Object[] row : raw) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];

            Map<String,Object> map = new HashMap<>();
            map.put("date",date);
            map.put("count",count);
            result.add(map);
        }

        System.out.println(result);

        return result;

    }

    public Event getEventById(User user , Long id) {

        Event event =  repo.findById(id).orElse(null);

        if (event == null) {
            throw new ResourceNotFoundException("Event with ID " + id + " not found.");
        }

        if (!event.getUser().equals(user)) {
            throw new SecurityException("Access denied to this event.");
        }

        return event;
    }

    public List<Event> getAllEventsForCurrentUser(User user) {

        log.info("Current authenticated user: {}", user != null ? user.getEmail() : "null");
        return repo.findByUser(user);

    }


    public Event createEvent(User user,EventRequest eventRequest) {

        if (eventRequest == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Event createdEvent = new Event();
        createdEvent.setTitle(eventRequest.getTitle());
        createdEvent.setDescription(eventRequest.getDescription());
        createdEvent.setEventDate(eventRequest.getEventDate());
        createdEvent.setReminderTime(eventRequest.getReminderTime());
        createdEvent.setRecurrenceType(eventRequest.getRecurrenceType());
        createdEvent.setRecurrenceInterval(eventRequest.getRecurrenceInterval());
        createdEvent.setRecurrenceEndDate(eventRequest.getRecurrenceEndDate());
        createdEvent.setUser(user);

        return repo.save(createdEvent);
    }

    public Event updateEvent(User user,Long id , EventRequest updatedEvent) {
        Event event = repo.findById(id).orElse(null);

        if (event == null || !event.getUser().equals(user)) return null;

        event.setTitle(updatedEvent.getTitle());
        event.setDescription(updatedEvent.getDescription());
        event.setEventDate(updatedEvent.getEventDate());
        event.setReminderTime(updatedEvent.getReminderTime());
        event.setRecurrenceType(updatedEvent.getRecurrenceType());
        event.setRecurrenceInterval(updatedEvent.getRecurrenceInterval());
        event.setRecurrenceEndDate(updatedEvent.getRecurrenceEndDate());
        event.setReminderSent(false);
        event.setReminderSentTime(null);

        return repo.save(event);
    }

    public void deleteEvent(User user,Long id) {
        Event event = repo.findById(id).orElse(null);

        if (event == null || !event.getUser().equals(user)) {
            throw new SecurityException("Not allowed to delete this event");
        }
        repo.delete(event);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkReminders() {

        LocalDateTime now = LocalDateTime.now().withNano(0);
        /*System.out.println("------------------------------------------------");
        System.out.println("checkReminders() running at: " + now);
        System.out.println("Local time: " + LocalDateTime.now());*/
        List<Event> dueEvents = repo.findPendingReminders(now);
        List<Long> okIds = new ArrayList<>();

        for(Event e : dueEvents) {
            //System.out.println("Scheduler running at: " + now);
            //System.out.println("Its time for event "+e.getTitle()+"("+e.getReminderTime()+")");
            try {
                String html = emailService.buildReminderHtml(e);
                emailService.sendReminderHtml(
                        e.getUser().getEmail(),
                        "Reminder: "+e.getTitle() ,
                        html
                );
                okIds.add(e.getId());

                //handle Recurrence
                createNextOccurenceIfRecurring(e);

            } catch (Exception ex) {
                log.error("Failed to send Email for event {}", e.getId(), ex);
            }
        }

        if (!okIds.isEmpty()) {
            int updated = repo.markRemindersSentByIds(okIds);
            //System.out.println("Proccessed "+updated+" reminders at "+now);
            log.info("Proccessed {} reminders at {} ",updated,now);
        }

    }

    private void createNextOccurenceIfRecurring(Event e) {
        if (e.getRecurrenceType() == null || e.getRecurrenceType() == RecurrenceType.NONE) {
            return;
        }

        int interval = (e.getRecurrenceInterval() != null && e.getRecurrenceInterval() > 0)
                ? e.getRecurrenceInterval()
                : 1;

        LocalDate nextDate = e.getEventDate();

        nextDate = addInterval(nextDate , e.getRecurrenceType(),interval);
        /*switch (e.getRecurrenceType()) {
            case DAILY -> nextDate = nextDate.plusDays(interval);
            case WEEKLY -> nextDate = nextDate.plusWeeks(interval);
            case MONTHLY -> nextDate = nextDate.plusMonths(interval);
            case YEARLY -> nextDate = nextDate.plusYears(interval);
        }*/

        if(e.getRecurrenceEndDate() != null && e.getRecurrenceEndDate().isBefore(nextDate)) {
            return;
        }

        Event next = new Event();

        next.setTitle(e.getTitle());
        next.setDescription(e.getDescription());
        next.setUser(e.getUser());
        next.setEventDate(nextDate);


        if (e.getReminderTime() != null ) {

            LocalDateTime nextReminder = e.getReminderTime();
            switch (e.getRecurrenceType()) {
                case DAILY -> nextReminder = nextReminder.plusDays(interval);
                case WEEKLY -> nextReminder = nextReminder.plusWeeks(interval);
                case MONTHLY -> nextReminder = nextReminder.plusMonths(interval);
                case YEARLY -> nextReminder = nextReminder.plusYears(interval);
            }
            next.setReminderTime(nextReminder);
        }

        next.setRecurrenceType(e.getRecurrenceType());
        next.setRecurrenceInterval(interval);
        next.setRecurrenceEndDate(e.getRecurrenceEndDate());

        repo.save(next);
    }

    public List<EventResponse> getCalendarEvents(User user,LocalDate start,LocalDate end) {

        List<EventResponse> result = new ArrayList<>();

        // single events in this range
        List<Event> singles = repo.findSinglesInRange(user,start,end);

        singles.forEach(e-> result.add(EventResponse.fromEntity(e)));

        // exception events in this range
        List<Event> exceptions = repo.findExceptionsInRange(user,start,end);
        exceptions.forEach(e -> result.add(EventResponse.fromEntity(e)));


        Map <Long , List<Event>> exceptionsByParent =  new HashMap<>();
        for (Event ex :  exceptions) {
            if (ex.getParentEventId() != null) {
                exceptionsByParent
                        .computeIfAbsent(ex.getParentEventId(), k -> new ArrayList<>())
                        .add(ex);
            }
        }

        // recurring masters
        List<Event> masters = repo.findRecurringMasterAffectingRange(user,start,end);


        for (Event master :  masters) {
            List<Event> exForThisMaster = exceptionsByParent.getOrDefault(master.getId(),List.of());

            result.addAll(
                expandMastersIntoOcurrences(master,start,end,exForThisMaster)
            );
        }


        return result;

    }

    private List<EventResponse> expandMastersIntoOcurrences(
            Event master,
            LocalDate rangeStart, LocalDate rangeEnd,
            List<Event> exceptions
    ) {

        List<EventResponse> list = new ArrayList<>();

        int interval = (master.getRecurrenceInterval() != null && master.getRecurrenceInterval() > 0)
                ? master.getRecurrenceInterval() : 1;

        LocalDate cursor = master.getEventDate();

        //bring cursor forward to first visible date
        while (cursor.isBefore(rangeStart)) {
            cursor = addInterval(cursor, master.getRecurrenceType(), interval);
        }

        //master series ends either at its recurrenceEndDate or at rangeEnd
        LocalDate limit = master.getRecurrenceEndDate() != null &&
                master.getRecurrenceEndDate().isBefore(rangeEnd)
                ? master.getRecurrenceEndDate()
                : rangeEnd;

        // create indexed exception map
        Set<LocalDate> exceptionDates = exceptions.stream()
                .map(Event::getOriginalDate)
                .collect(Collectors.toSet());

        //expand occurrences
        while (!cursor.isAfter(limit)) {

            if (!exceptionDates.contains(cursor)) {
                list.add(createOccurrenceFromMaster(master, cursor));
            }

            cursor = addInterval(cursor, master.getRecurrenceType(), interval);
        }

        return list;
    }

    @Transactional
    public void moveOccurrence(User user, Long masterId, MoveOccurrenceRequest req) {

        Event master = repo.findById(masterId)
                .orElseThrow(()-> new BadRequestException("Event with id " + masterId + " not found."));

        if (!master.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to move this event.");
        }

        LocalDate newDate = req.getNewDate();
        LocalDate originalDate = req.getOriginalDate();

        if (originalDate == null || newDate == null) {
            throw new BadRequestException("originalDate and newDate are required");
        }
        validateEventDate(newDate);

        String mode = req.getMode().toUpperCase();

        switch (mode) {
            case "SINGLE":
                moveSingleOcurrence(user, master, originalDate , newDate);
                break;

            case "THIS_AND_FUTURE":
                moveThisAndFuture(user, master, originalDate, newDate);
                break;

            case "ALL":
                moveAllOcurrences(user, master, newDate);
                break;

            default:
                throw new BadRequestException("Unknown mode: " + mode);
        }
    }

    private void moveSingleOcurrence(User user ,Event master,LocalDate originalDate, LocalDate newDate) {

        validateEventDate(newDate);

        if (master.getRecurrenceEndDate() != null &&
                newDate.isAfter(master.getRecurrenceEndDate() )) {
            throw new BadRequestException("New date cannot be after recurrence end date.");
        }


        Event ex = new Event();
        ex.setTitle(master.getTitle());
        ex.setDescription(master.getDescription());
        ex.setUser(master.getUser());

        ex.setOriginalDate(originalDate);
        ex.setParentEventId(master.getId());
        ex.setException(true);

        if (master.getReminderTime() != null) {
            ex.setReminderTime(updateReminderTime(master.getReminderTime(), master.getEventDate(), newDate));
        }

        if (originalDate.isEqual(master.getEventDate())) {

            LocalDate newMasterDate = addInterval(
                    master.getEventDate(),
                    master.getRecurrenceType(),
                    master.getRecurrenceInterval()
            );

            if (master.getReminderTime() != null) {
                master.setReminderTime(updateReminderTime(master.getReminderTime(), master.getEventDate(), newMasterDate));
            }

            master.setEventDate(newMasterDate);

            ex.setOriginalDate(null);
            ex.setParentEventId(null);
            ex.setException(false);
        }


        ex.setEventDate(newDate);
        ex.setTitle(master.getTitle()+" # "+newDate);
        repo.save(ex);

    }

    private void moveThisAndFuture(User user ,Event master,LocalDate originalDate, LocalDate newStartDate) {

        validateEventDate(newStartDate);

        System.out.println("--------"+originalDate);

        LocalDate oldEnd = master.getRecurrenceEndDate();

        Event existingException = repo.findExceptionByParentAndOriginalDate(
                master.getId(), originalDate
        );

        Event newMaster;

        if (existingException != null) {

            existingException.setEventDate(null);
            repo.save(existingException);


            newMaster = repo.findMasterByParent(master.getId(), originalDate);
            if (newMaster == null) {
                throw new RuntimeException("Corrupted split state: new master missing");
            }


            updateMasterStartDate(newMaster, originalDate, newStartDate);

        } else {

            LocalDate cutEnd = originalDate.minusDays(1);
            master.setRecurrenceEndDate(cutEnd);
            repo.save(master);


            Event skip = new Event();
            skip.setUser(master.getUser());
            skip.setException(true);
            skip.setParentEventId(master.getId());
            skip.setOriginalDate(originalDate);
            skip.setEventDate(null);
            repo.save(skip);


            newMaster = new Event();
            newMaster.setUser(master.getUser());
            newMaster.setTitle(master.getTitle());
            newMaster.setDescription(master.getDescription());
            newMaster.setException(false);
            newMaster.setParentEventId(master.getId());
            newMaster.setRecurrenceType(master.getRecurrenceType());
            newMaster.setRecurrenceInterval(master.getRecurrenceInterval());
            newMaster.setRecurrenceEndDate(oldEnd);

            validateEventDate(newStartDate);

            if (master.getReminderTime() != null) {
                newMaster.setReminderTime(updateReminderTime(
                        master.getReminderTime(),
                        master.getEventDate(),
                        newStartDate
                ));
            }

            newMaster.setEventDate(newStartDate);
            //newMaster.setTitle(newMaster.getTitle()+" # "+newStartDate);
            repo.save(newMaster);
        }

        repo.deleteExceptionsForMasterAfter(master.getId(), originalDate);
    }

    private void updateMasterStartDate(Event newMaster, LocalDate originalDate, LocalDate newStartDate) {

        newMaster.setEventDate(newStartDate);

        if (newMaster.getReminderTime() != null) {
            newMaster.setReminderTime(updateReminderTime(
                    newMaster.getReminderTime(),
                    originalDate,
                    newStartDate
            ));
        }

        repo.save(newMaster);
    }

    private  void moveAllOcurrences(User user ,Event master , LocalDate newStartDate) {

        validateEventDate(newStartDate);

        if (master.getRecurrenceEndDate() != null &&
                newStartDate.isAfter(master.getRecurrenceEndDate())) {
            throw new BadRequestException("New date is after recurrence end date.");
        }

        LocalDate start = master.getEventDate();
        LocalDate end = master.getRecurrenceEndDate();

        Event realMaster = repo.findRecurringMasterAffectingRangeByDate(user, master.getId());

        if (realMaster != null) {

            LocalDate d =  realMaster.getEventDate();
            LocalDate currD = realMaster.getEventDate();
            while (d.isBefore(newStartDate)) {
                currD = d;
                d = addInterval(d, realMaster.getRecurrenceType(), realMaster.getRecurrenceInterval());
            }

            //System.out.println("----currD:---"+currD);
            //System.out.println("----d:---"+d);

            long diff1 = ChronoUnit.DAYS.between(currD, newStartDate);
            long diff2 = ChronoUnit.DAYS.between(d, newStartDate);
            diff1 = Math.abs(diff1);
            diff2 = Math.abs(diff2);
            //System.out.println("----diff1:---"+diff1);
            //System.out.println("----diff2:---"+diff2);

            LocalDate realNewStartDate;
            LocalDate realNewRecurrenceEndDate;

            if (diff1 < diff2) {

                realNewStartDate = realMaster.getEventDate().plusDays(diff1);
                realNewRecurrenceEndDate = realMaster.getRecurrenceEndDate().plusDays(diff1);
            } else {

                realNewStartDate = realMaster.getEventDate().minusDays(diff2);
                realNewRecurrenceEndDate = realMaster.getRecurrenceEndDate().minusDays(diff2);
            }


            validateEventDate(realNewStartDate);

            if (realMaster.getReminderTime() != null) {
                realMaster.setReminderTime(updateReminderTime(
                        realMaster.getReminderTime(),
                        realMaster.getEventDate(),
                        realNewStartDate
                ));
            }

            realMaster.setEventDate(realNewStartDate);
            realMaster.setRecurrenceEndDate(realNewRecurrenceEndDate);
        }

        repo.save(master);
        repo.deleteExceptionsOfMaster(master.getId());
    }

    private void validateEventDate(LocalDate newDate) {
        LocalDate minDate = LocalDate.now().plusDays(1);
        if (newDate.isBefore(minDate)) {
            throw new BadRequestException("Event date must be at least tomorrow.");
        }
    }

    private void validateReminderNotPast(LocalDateTime reminder) {
        if (reminder.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reminder time must be after current datetime.");
        }
    }

    /*private void moveExistingException(Event ex, LocalDate newDate) {

        validateEventDate(newDate);

        if (ex.getReminderTime() != null) {
            ex.setReminderTime(updateReminderTime(ex.getReminderTime(), ex.getEventDate(), newDate));
        }

        ex.setEventDate(newDate);
        ex.setReminderSent(false);
        ex.setReminderSentTime(null);

        repo.save(ex);

    }*/

    public void moveEventDate(User user,Long  eventId, LocalDate newDate) {

        Event e = repo.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found"));


        if (!e.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to move this event.");
        }

        validateEventDate(newDate);


        if (e.getReminderTime() != null) {
            e.setReminderTime(updateReminderTime(e.getReminderTime(), e.getEventDate(), newDate));
        }

        e.setEventDate(newDate);
        repo.save(e);
    }

    private LocalDate addInterval(LocalDate d, RecurrenceType type, int interval) {
        return switch (type) {
            case DAILY -> d.plusDays(interval);
            case WEEKLY -> d.plusWeeks(interval);
            case MONTHLY -> d.plusMonths(interval);
            case YEARLY -> d.plusYears(interval);
            default -> d;
        };
    }

    private LocalDateTime updateReminderTime(LocalDateTime oldReminder,
                                             LocalDate oldEventDate,
                                             LocalDate newEventDate) {

        long daysBetween = DAYS.between(oldReminder.toLocalDate(), oldEventDate);

        // Reminder new = Event new - same days gap
        LocalDate newReminderDate = newEventDate.minusDays(daysBetween);

        LocalDateTime newReminder = LocalDateTime.of(newReminderDate, oldReminder.toLocalTime());

        validateReminderNotPast(newReminder);

        return newReminder;
    }

    private EventResponse createOccurrenceFromMaster(Event master, LocalDate date) {
        EventResponse dto = new EventResponse();

        dto.setId(master.getId());
        dto.setTitle(master.getTitle());
        dto.setDescription(master.getDescription());

        if (master.getReminderTime() != null) {
            dto.setReminderTime(updateReminderTime(master.getReminderTime(),master.getEventDate(), date));
        }

        dto.setEventDate(date);
        dto.setRecurrenceType(master.getRecurrenceType());
        dto.setRecurrenceInterval(master.getRecurrenceInterval());
        dto.setRecurrenceEndDate(master.getRecurrenceEndDate());

        dto.setParentEventId(master.getId());
        dto.setException(false);
        dto.setOriginalDate(date);

        return dto;
    }

}
