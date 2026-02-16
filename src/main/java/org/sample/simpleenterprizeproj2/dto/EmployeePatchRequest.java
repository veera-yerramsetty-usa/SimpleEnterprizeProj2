package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeePatchRequest {

    @Size(min = 1, max = 255, message = "First name must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "First name must contain only letters, spaces, hyphens, or apostrophes")
    private String firstName;

    @Size(min = 1, max = 255, message = "Last name must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "Last name must contain only letters, spaces, hyphens, or apostrophes")
    private String lastName;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 255, message = "Phone must not exceed 255 characters")
    @Pattern(regexp = "^[+]?[0-9() -]+$", message = "Phone must contain only digits, spaces, +, -, or parentheses")
    private String phone;

    private Long departmentId;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
