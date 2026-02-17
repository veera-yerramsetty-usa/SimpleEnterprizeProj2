package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.EmployeePatchRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.exception.ServiceUnavailableException;
import org.sample.simpleenterprizeproj2.mapper.EmployeeMapper;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.repository.EmployeeRepository;
import org.sample.simpleenterprizeproj2.specification.EmployeeSpecification;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "employeeService", fallbackMethod = "findAllFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
    public Page<EmployeeResponse> findAll(String firstName, String lastName, String email,
                                          Long departmentId, Pageable pageable) {
        return employeeRepository.findAll(
                        EmployeeSpecification.build(firstName, lastName, email, departmentId), pageable)
                .map(employeeMapper::toResponse);
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "findResponseByIdFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
    public EmployeeResponse findResponseById(Long id) {
        return employeeMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "findEntityByIdFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
    public Employee findEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "createFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = employeeMapper.toEntity(request);
        resolveDepartment(employee, request.getDepartmentId());
        EmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        log.info("Created employee id={}", response.getId());
        return response;
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "updateFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEntityById(id);
        employeeMapper.updateEntity(employee, request);
        resolveDepartment(employee, request.getDepartmentId());
        EmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        log.info("Updated employee id={}", id);
        return response;
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "patchFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
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

    @CircuitBreaker(name = "employeeService", fallbackMethod = "deleteFallback")
    @Bulkhead(name = "employeeService")
    @Retry(name = "employeeService")
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

    // ── Fallback methods ──────────────────────────────────────────────

    private Page<EmployeeResponse> findAllFallback(String firstName, String lastName, String email,
                                                   Long departmentId, Pageable pageable, Throwable t) {
        log.warn("Fallback for findAll triggered: {}", t.getMessage());
        return Page.empty(pageable);
    }

    private EmployeeResponse findResponseByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findResponseById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }

    private Employee findEntityByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findEntityById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }

    private EmployeeResponse createFallback(EmployeeRequest request, Throwable t) {
        log.warn("Fallback for create triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }

    private EmployeeResponse updateFallback(Long id, EmployeeRequest request, Throwable t) {
        log.warn("Fallback for update(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }

    private EmployeeResponse patchFallback(Long id, EmployeePatchRequest request, Throwable t) {
        log.warn("Fallback for patch(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }

    private void deleteFallback(Long id, Throwable t) {
        log.warn("Fallback for delete(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Employee service is temporarily unavailable", t);
    }
}
