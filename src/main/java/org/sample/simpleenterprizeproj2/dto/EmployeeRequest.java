package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeRequest {

    @NotBlank(message = "{validation.employee.firstName.required}")
    @Size(max = 255, message = "{validation.employee.firstName.size}")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "{validation.employee.firstName.pattern}")
    private String firstName;

    @NotBlank(message = "{validation.employee.lastName.required}")
    @Size(max = 255, message = "{validation.employee.lastName.size}")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "{validation.employee.lastName.pattern}")
    private String lastName;

    @NotBlank(message = "{validation.employee.email.required}")
    @Email(message = "{validation.employee.email.valid}")
    @Size(max = 255, message = "{validation.employee.email.size}")
    private String email;

    @Size(max = 255, message = "{validation.employee.phone.size}")
    @Pattern(regexp = "^[+]?[0-9() -]+$", message = "{validation.employee.phone.pattern}")
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
