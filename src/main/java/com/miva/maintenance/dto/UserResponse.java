package com.miva.maintenance.dto;

import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Safe, password-free view of a User for API responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private Role role;
    private String department;
    private List<String> categoryIds;
    private boolean active;
    private boolean mustChangePassword;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .department(u.getDepartment())
                .categoryIds(u.getCategoryIds())
                .active(u.isActive())
                .mustChangePassword(u.isMustChangePassword())
                .build();
    }
}
