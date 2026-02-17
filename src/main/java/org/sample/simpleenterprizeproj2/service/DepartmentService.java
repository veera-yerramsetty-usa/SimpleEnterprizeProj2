package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.DepartmentPatchRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.mapper.DepartmentMapper;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.repository.DepartmentRepository;
import org.sample.simpleenterprizeproj2.specification.DepartmentSpecification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @CircuitBreaker(name = "departmentService")
    public Page<DepartmentResponse> findAll(String name, Pageable pageable) {
        return departmentRepository.findAll(DepartmentSpecification.build(name), pageable)
                .map(departmentMapper::toResponse);
    }

    @CircuitBreaker(name = "departmentService")
    public DepartmentResponse findResponseById(Long id) {
        return departmentMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "departmentService")
    public Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    @CircuitBreaker(name = "departmentService")
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = departmentMapper.toEntity(request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Created department id={}", response.getId());
        return response;
    }

    @CircuitBreaker(name = "departmentService")
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findEntityById(id);
        departmentMapper.updateEntity(department, request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Updated department id={}", id);
        return response;
    }

    @CircuitBreaker(name = "departmentService")
    @Transactional
    public DepartmentResponse patch(Long id, DepartmentPatchRequest request) {
        Department department = findEntityById(id);
        departmentMapper.patchEntity(department, request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Patched department id={}", id);
        return response;
    }

    @CircuitBreaker(name = "departmentService")
    @Transactional
    public void delete(Long id) {
        Department department = findEntityById(id);
        department.setDeleted(true);
        departmentRepository.save(department);
        log.info("Soft-deleted department id={}", id);
    }
}
