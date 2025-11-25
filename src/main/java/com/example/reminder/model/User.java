package com.example.reminder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , unique = true)
    private String email;

    @Column(nullable = false)
    private String password;


    private boolean enabled = true;

    private boolean emailVerified = true;
    private String pendingEmail;
    private String emailVerificationToken;
    private LocalDateTime emailVerificationTokenExpiry;


    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    // connect to mid-table (UserRole)
    @OneToMany(mappedBy = "user" , cascade = CascadeType.ALL , orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    // connect to mid-table (Event)
    @OneToMany(mappedBy = "user" , cascade = CascadeType.ALL , orphanRemoval = true)
    private Set<Event> events = new HashSet<>();

    /** add role for a user */
    public void addRole(Role role) {
        UserRole userRole = new UserRole(this, role);
        userRoles.add(userRole);
        role.getUserRoles().add(userRole);
    }

    /** delete role from a user */
    public void removeRole(Role role) {
        userRoles.removeIf(ur -> ur.getRole().equals(role));
        role.getUserRoles().removeIf(ur -> ur.getUser().equals(this));
    }

    public Set<String> getRolesAsString() {
        return this.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
    }
}
