package org.sample.simpleenterprizeproj2.dto;

public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private DepartmentResponse department;

    public EmployeeResponse() {}

    public EmployeeResponse(Long id, String firstName, String lastName, String email, String phone, DepartmentResponse department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public DepartmentResponse getDepartment() { return department; }
    public void setDepartment(DepartmentResponse department) { this.department = department; }
}
