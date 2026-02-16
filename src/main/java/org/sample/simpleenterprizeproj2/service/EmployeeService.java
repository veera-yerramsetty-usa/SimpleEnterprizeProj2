package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.repository.DepartmentRepository;
import org.sample.simpleenterprizeproj2.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @Transactional
    public Employee create(Employee employee) {
        if (employee.getDepartment() != null && employee.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(employee.getDepartment().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + employee.getDepartment().getId()));
            employee.setDepartment(dept);
        }
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, Employee updated) {
        Employee employee = findById(id);
        employee.setFirstName(updated.getFirstName());
        employee.setLastName(updated.getLastName());
        employee.setEmail(updated.getEmail());
        employee.setPhone(updated.getPhone());
        if (updated.getDepartment() != null && updated.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(updated.getDepartment().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + updated.getDepartment().getId()));
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }
        return employeeRepository.save(employee);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findById(id);
        employee.setDeleted(true);
        employeeRepository.save(employee);
    }
}
