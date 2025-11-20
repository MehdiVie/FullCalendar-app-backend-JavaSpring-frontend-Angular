package com.example.reminder.controller;

import com.example.reminder.dto.*;
import com.example.reminder.model.User;
import com.example.reminder.security.CustomUserDetails;
import com.example.reminder.security.JwtService;
import com.example.reminder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);

        // generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getRolesAsString());

        // build Auth Response
        AuthResponse authResponse = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRolesAsString(),
                token
        );

        // build Api Response
        ApiResponse<AuthResponse> response = new ApiResponse<>(
          "success" ,
          "User registered successfully",
                authResponse
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {

        try {
            var theToken = new UsernamePasswordAuthenticationToken(request.getEmail() , request.getPassword());
            Authentication auth = authenticationManager.authenticate(theToken);

            CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();

            String token = jwtService.generateToken(cud.getUsername() , cud.getUser().getRolesAsString());

            AuthResponse authResponse = new AuthResponse(
                    cud.getId(),
                    cud.getUsername(),
                    cud.getUser().getRolesAsString(),
                    token
            );

            // build Api Response
            ApiResponse<AuthResponse> response = new ApiResponse<>(
                    "success" ,
                    "User loggedIn successfully",
                    authResponse
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {

            ApiResponse<AuthResponse> error = new ApiResponse<>(
                    "error",
                    "Invalid email or password",
                     null
            );
            return ResponseEntity.status(401).body(error);
        }


    }
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {

        String verifiedEmail = userService.getUserByVerificationToken(token);


        if (verifiedEmail != null && !verifiedEmail.isEmpty()) return ResponseEntity.ok(
                new ApiResponse<>("success", "Email verified successfully.", verifiedEmail)
        );

        return ResponseEntity.ok(
                new ApiResponse<>("error", "Failed email verification.", null)
        );

    }
}
