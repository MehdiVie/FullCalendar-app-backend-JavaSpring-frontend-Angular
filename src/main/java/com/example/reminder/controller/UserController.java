package com.example.reminder.controller;
import com.example.reminder.dto.*;
import com.example.reminder.model.User;
import com.example.reminder.security.AuthContext;
import com.example.reminder.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final AuthContext authContext;
    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfiöe() {
        User currentUser = authContext.getCurrentUser();
        UserProfileResponse profile = new UserProfileResponse(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRolesAsString()
        );
        return ResponseEntity.ok(
                new ApiResponse<>("success","profile fetched", profile));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<UserProfileResponse>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
            )
    {
        User currentUser = authContext.getCurrentUser();

        userService.changePassword(currentUser,request.getOldPassword(),request.getNewPassword());

        return ResponseEntity.ok(
                new ApiResponse<>("success", "Password changed successfully.", null)
        );
    }

    @PutMapping("/change-email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> changeEmail(
            @RequestBody @Valid ChangeEmailRequest request
    )
    {
        User currentUser = authContext.getCurrentUser();

        userService.changeEmail(currentUser,request.getOldEmail(),request.getNewEmail());

        return ResponseEntity.ok(
                new ApiResponse<>("success", "Check " + request.getNewEmail() + " " +
                        "to verify the new Email.", null)
        );
    }

}
