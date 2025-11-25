package com.example.reminder.dto;

import com.example.reminder.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private Set<String> roles;

    public static UserProfileResponse fromEntity(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getRolesAsString()
        );
    }
}
