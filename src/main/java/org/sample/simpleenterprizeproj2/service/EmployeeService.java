package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.EmployeePatchRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.mapper.EmployeeMapper;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.repository.EmployeeRepository;
import org.sample.simpleenterprizeproj2.specification.EmployeeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
        this.employeeMapper = employeeMapper;
    }

    public Page<EmployeeResponse> findAll(String firstName, String lastName, String email,
                                          Long departmentId, Pageable pageable) {
        return employeeRepository.findAll(
                        EmployeeSpecification.build(firstName, lastName, email, departmentId), pageable)
                .map(employeeMapper::toResponse);
    }

    public EmployeeResponse findResponseById(Long id) {
        return employeeMapper.toResponse(findEntityById(id));
    }

    public Employee findEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = employeeMapper.toEntity(request);
        resolveDepartment(employee, request.getDepartmentId());
        EmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        log.info("Created employee id={}", response.getId());
        return response;
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEntityById(id);
        employeeMapper.updateEntity(employee, request);
        resolveDepartment(employee, request.getDepartmentId());
        EmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        log.info("Updated employee id={}", id);
        return response;
    }

    @Transactional
    public EmployeeResponse patch(Long id, EmployeePatchRequest request) {
        Employee employee = findEntityById(id);
        employeeMapper.patchEntity(employee, request);
        if (request.getDepartmentId() != null) {
            resolveDepartment(employee, request.getDepartmentId());
        }
        EmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        log.info("Patched employee id={}", id);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findEntityById(id);
        employee.setDeleted(true);
        employeeRepository.save(employee);
        log.info("Soft-deleted employee id={}", id);
    }

    private void resolveDepartment(Employee employee, Long departmentId) {
        if (departmentId != null) {
            Department dept = departmentService.findEntityById(departmentId);
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }
    }
}
