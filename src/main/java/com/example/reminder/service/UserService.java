package com.example.reminder.service;
import com.example.reminder.dto.EmailVerificationResult;
import com.example.reminder.dto.RegisterRequest;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.repository.RoleRepository;
import com.example.reminder.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class UserService {


    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public UserService(UserRepository userRepo, RoleRepository roleRepo,@Lazy PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /*
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepo.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(
                                user.getUserRoles().stream()
                                        .map(role -> "ROLE_" + role.getRole().getName())
                                        .toArray(String[]::new)
                        )
                        .build()
                )
                .orElseThrow(() ->
                new UsernameNotFoundException("User not found with email: " + email)
                );
    }*/


    public User registerUser(RegisterRequest request) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Role role = roleRepo.findByName("USER")
                    .orElseGet(()-> {
                        Role newRole = new Role();
                        newRole.setName("USER");
                        return  roleRepo.save(newRole);
                    });
        // create User
        User user =new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // add Role
        user.addRole(role);

        return userRepo.save(user);
    }

    public void changePassword(User user,String oldPassword, String newPassword) {

        if (!passwordEncoder.matches(oldPassword, user.getPassword() )) {
            throw new BadRequestException("Old password is incorrect.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public void changeEmail(User user,String oldEmail, String newEmail) {

        if (!(oldEmail.equalsIgnoreCase(user.getEmail())) ) {
            throw new BadRequestException("Current email is incorrect.");
        }

        if (userRepo.existsByEmail(newEmail)) {
            throw new BadRequestException("New email already exists");
        }

        if (newEmail == null || newEmail.length() < 6) {
            throw new BadRequestException("New Email must be at least 6 characters.");
        }

        String token = UUID.randomUUID().toString();

        user.setPendingEmail(newEmail);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusMinutes(30));

        String link = frontendBaseUrl+"/login?verify="+token;

        try {
            String html = emailService.buildVerificationEmailHtml(link,newEmail);
            emailService.sendReminderHtml(
                    newEmail,
                    "Reminder App: Verify your New Email." ,
                    html
            );

        } catch (Exception ex) {
            log.error("Failed to send Email for changed Email {}", newEmail, ex);
        }
        userRepo.save(user);
    }

    public EmailVerificationResult getUserByEmailVerificationToken(String token) {
        User user= userRepo.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token."));

        boolean expired = false;
        if (!userRepo.isEmailVerificationExpired(user.getEmail())) {
            user.setEmail(user.getPendingEmail());
        } else {
            expired = true;
        }
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        userRepo.save(user);

        return new EmailVerificationResult(
                user.getEmail(),
                expired
        );
    }

    public void requestPasswordReset(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()->new BadRequestException("Invalid Email address."));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry =  LocalDateTime.now().plusMinutes(30); // resetpass link expiration in 1 hour

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(expiry);

        String link = frontendBaseUrl+"/reset-password?token="+token;
        String html = emailService.buildResetPasswordHtml(link);

        try {

            emailService.sendReminderHtml(
                    user.getEmail(),
                    "Reminder App: Reset your Password." ,
                    html
            );

        } catch (Exception ex) {
            log.error("Failed to send Email for reset password for user {}", user.getEmail(), ex);
        }

        userRepo.save(user);

    }

    public User resetPasswordCheckToken(String token) {
        return userRepo.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset password token."));
    }

    public  void resetPassword(String token, String newPassword) {

        User user = this.resetPasswordCheckToken(token);

        if(user.getResetPasswordTokenExpiry() == null ||
           user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Expired reset password token.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordTokenExpiry(null);
        user.setResetPasswordToken(null);
        userRepo.save(user);

    }

}
