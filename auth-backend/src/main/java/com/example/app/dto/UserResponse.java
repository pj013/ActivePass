package com.example.app.dto;

import com.example.app.model.Role;
import com.example.app.model.User;

public record UserResponse(Long id, String fullName, String email, Role role, String dashboardPath) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getRole().getDashboardPath());
    }
}
