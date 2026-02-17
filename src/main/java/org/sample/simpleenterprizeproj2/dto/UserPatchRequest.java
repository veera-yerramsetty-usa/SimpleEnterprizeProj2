package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserPatchRequest {

    @Size(min = 1, max = 255, message = "{validation.user.username.patch.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "{validation.user.username.pattern}")
    private String username;

    @Email(message = "{validation.user.email.valid}")
    @Size(max = 255, message = "{validation.user.email.size}")
    private String email;

    @Size(min = 8, max = 255, message = "{validation.user.password.size}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "{validation.user.password.pattern}")
    private String password;

    @Size(max = 255, message = "{validation.user.role.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{validation.user.role.pattern}")
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
