package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserPatchRequest {

    @Size(min = 1, max = 255, message = "Username must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username must contain only alphanumeric characters, underscores, or hyphens")
    private String username;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @Size(max = 255, message = "Role must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Role must contain only alphanumeric characters or underscores")
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
