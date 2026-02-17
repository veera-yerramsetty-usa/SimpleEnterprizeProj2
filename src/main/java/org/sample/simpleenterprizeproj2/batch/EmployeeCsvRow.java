package org.sample.simpleenterprizeproj2.batch;

public record EmployeeCsvRow(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String departmentName
) {}
