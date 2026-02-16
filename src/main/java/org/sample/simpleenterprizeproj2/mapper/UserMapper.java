package org.sample.simpleenterprizeproj2.mapper;

import org.sample.simpleenterprizeproj2.dto.UserPatchRequest;
import org.sample.simpleenterprizeproj2.dto.UserRequest;
import org.sample.simpleenterprizeproj2.dto.UserResponse;
import org.sample.simpleenterprizeproj2.model.User;
import org.sample.simpleenterprizeproj2.util.SanitizationUtils;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        user.setPassword(request.getPassword());
        user.setRole(SanitizationUtils.sanitize(request.getRole()));
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    public void updateEntity(User user, UserRequest request) {
        user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        user.setPassword(request.getPassword());
        user.setRole(SanitizationUtils.sanitize(request.getRole()));
    }

    public void patchEntity(User user, UserPatchRequest request) {
        if (request.getUsername() != null) {
            user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        }
        if (request.getEmail() != null) {
            user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }
        if (request.getRole() != null) {
            user.setRole(SanitizationUtils.sanitize(request.getRole()));
        }
    }
}
