package org.sample.simpleenterprizeproj2.mapper;

import org.sample.simpleenterprizeproj2.dto.EmployeePatchRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeResponse;
import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.util.SanitizationUtils;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    private final DepartmentMapper departmentMapper;

    public EmployeeMapper(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    public Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setFirstName(SanitizationUtils.sanitize(request.getFirstName()));
        employee.setLastName(SanitizationUtils.sanitize(request.getLastName()));
        employee.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        employee.setPhone(SanitizationUtils.sanitize(request.getPhone()));
        return employee;
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDepartment() != null
                        ? departmentMapper.toResponse(employee.getDepartment())
                        : null
        );
    }

    public void updateEntity(Employee employee, EmployeeRequest request) {
        employee.setFirstName(SanitizationUtils.sanitize(request.getFirstName()));
        employee.setLastName(SanitizationUtils.sanitize(request.getLastName()));
        employee.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        employee.setPhone(SanitizationUtils.sanitize(request.getPhone()));
    }

    public void patchEntity(Employee employee, EmployeePatchRequest request) {
        if (request.getFirstName() != null) {
            employee.setFirstName(SanitizationUtils.sanitize(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            employee.setLastName(SanitizationUtils.sanitize(request.getLastName()));
        }
        if (request.getEmail() != null) {
            employee.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        }
        if (request.getPhone() != null) {
            employee.setPhone(SanitizationUtils.sanitize(request.getPhone()));
        }
    }
}
