package com.example.reminder.repository;

import com.example.reminder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u " +
            "left join fetch u.userRoles ur " +
            "Left Join fetch ur.role r " +
            "where emailVerificationToken = :token AND emailVerified = false")
    User findByEmailVerificationToken(String token);



    @Query("select u from User u " +
            "left join fetch u.userRoles ur " +
            "Left Join fetch ur.role r " +
            "where email = :email")
    Optional<User> findByEmailWithRoles (String email);
}
