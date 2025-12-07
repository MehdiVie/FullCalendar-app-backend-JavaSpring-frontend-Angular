package com.example.reminder.repository;

import com.example.reminder.model.Event;
import com.example.reminder.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByUser(@Param("user") User user);

    Page<Event> findByUser(@Param("user") User user, Pageable pageable);

    Page<Event> findAll(Pageable pageable);

    // return all Events after specific date
    @Query("SELECT e FROM Event e WHERE e.eventDate >= :date")
    Page<Event> findAllAfterDate(@Param("date") LocalDate date , Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.user=:user AND e.eventDate >= :date " +
            "AND (LOWER(title) LIKE :search OR " +
            "LOWER(description) LIKE :search)")
    Page<Event> findByUserAndAfterDateAndSearch(@Param("user") User user, @Param("date")LocalDate date ,
                                                @Param("search") String search , Pageable pageable);
    // return all Events after specific date
    @Query("SELECT e FROM Event e WHERE e.user=:user AND e.eventDate >= :date")
    Page<Event> findByUserAndAfterDate(@Param("user") User user, @Param("date")LocalDate date ,
                                       Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.user=:user " +
            "AND (LOWER(title) LIKE :search OR " +
            "LOWER(description) LIKE :search)")
    Page<Event> findByUserAndSearch(@Param("user") User user,@Param("search") String search , Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventDate >= :date " +
            "AND (LOWER(title) LIKE :search OR " +
            "LOWER(description) LIKE :search)")
    Page<Event> findAllEventsAndAfterDateAndSearch(@Param("date")LocalDate date ,
                                                @Param("search") String search , Pageable pageable);
    @Query("SELECT e FROM Event e WHERE  " +
            " (LOWER(title) LIKE :search OR " +
            "LOWER(description) LIKE :search)")
    Page<Event> findAllEventsAndSearch(@Param("search") String search , Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventDate >= :date")
    Page<Event> findAllEventsAndAfterDate(@Param("date")LocalDate date ,
                                       Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.reminderSent = false AND e.reminderTime <= :now ")
    List<Event> findPendingReminders(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.user=:user AND e.reminderSent = false AND " +
                                    "e.reminderTime <= :threshold  AND e.reminderTime >= :now ")
    List<Event> findAllUpcomingReminders(@Param("user") User user, @Param("now") LocalDateTime now,
                                      @Param("threshold")LocalDateTime threshold);

    @Query("SELECT e FROM Event e WHERE e.user=:user AND e.reminderSent = true AND " +
            "e.reminderTime >= :threshold  AND e.reminderTime <= :now ")
    List<Event> findAllSentReminders(@Param("user") User user, @Param("now") LocalDateTime now,
                                         @Param("threshold")LocalDateTime threshold);

    @Modifying(clearAutomatically = true , flushAutomatically = true)
    @Query("UPDATE Event e SET reminderSent=true , reminderSentTime=now() WHERE e.id in :ids")
    int markRemindersSentByIds(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.reminderSent = true")
    Long countByReminderSentTrue();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.reminderSent = false")
    Long countByReminderSentFalse();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdAt >= :from")
    Long countEventsCreatedAfter(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.reminderSent=true AND e.reminderSentTime >= :from")
    Long countReminderSentAfter(@Param("from") LocalDateTime from);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.reminderSent=false AND e.reminderTime >= :from" +
            " AND e.reminderTime <= :to")
    Long countReminderSentBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT e.eventDate as date , COUNT(e) as cnt FROM Event e WHERE " +
            " e.eventDate >= :from AND e.eventDate <= :to GROUP BY  e.eventDate ORDER BY  e.eventDate")
    List<Object[]> eventsPerDaySince(@Param("from") LocalDate from,@Param("to") LocalDate to);

    /*@Query("SELECT e FROM Event e where e.user = :user AND" +
            " e.eventDate between :start AND :end" +
            " order by e.eventDate asc" )
    List<Event> findByUserAndEventDateBetween (@Param("user") User user
            ,@Param("start") LocalDate start ,@Param("end") LocalDate end);*/


    @Query("SELECT e from Event e where e.user = :user " +
            " AND e.isException = false AND e.recurrenceType = 'NONE'" +
            " AND e.eventDate between :start AND :end ")
    List<Event> findSinglesInRange(@Param("user") User user
            ,@Param("start") LocalDate start ,@Param("end") LocalDate end);

    @Query("SELECT e from Event e where e.user = :user " +
            " AND e.isException = false AND e.recurrenceType <> 'NONE' AND e.eventDate <= :end " +
            " AND (e.recurrenceEndDate is null OR e.recurrenceEndDate >= :start ) ")
    List<Event> findRecurringMasterAffectingRange(@Param("user") User user
            ,@Param("start") LocalDate start ,@Param("end") LocalDate end);

    @Query("SELECT e from Event e where e.user = :user " +
            " AND e.isException = false AND e.recurrenceType <> 'NONE'  " +
            " AND e.id = :id  ")
    Event findRecurringMasterAffectingRangeByDate(@Param("user") User user, @Param("id") long id );

    @Query("SELECT e from Event e where e.user = :user " +
            " AND e.isException = true " +
            " AND e.eventDate between :start AND :end ")
    List<Event> findExceptionsInRange(@Param("user") User user
            ,@Param("start") LocalDate start ,@Param("end") LocalDate end);

/*
    Optional<Event> findByParentEventIdAndOriginalDate(@Param("parentEventId") Long parentEventId,
                                                       @Param("originalDate") LocalDate originalDate);*/

    @Modifying
    @Transactional
    @Query("DELETE FROM Event e WHERE e.parentEventId = :masterId " +
            " AND e.isException = true ")
    int deleteExceptionsOfMaster(@Param("masterId") Long masterId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Event e WHERE e.parentEventId = :masterId " +
            " AND e.isException = true AND e.originalDate > :originalDate ")
    int deleteExceptionsForMasterAfter(@Param("masterId") Long masterId,
                                       @Param("originalDate") LocalDate originalDate);

    @Query("select e from Event e where e.isException = true and e.parentEventId = :parentId and e.originalDate = :date")
    Event findExceptionByParentAndOriginalDate(Long parentId, LocalDate date);


    @Query("select e from Event e where e.isException = false and e.parentEventId = :parentId and e.eventDate = :date")
    Event findMasterByParent(Long parentId, LocalDate date);

}
