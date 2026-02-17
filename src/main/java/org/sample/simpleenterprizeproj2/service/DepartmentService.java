package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.BulkUpdateRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentPatchRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.exception.ServiceUnavailableException;
import org.sample.simpleenterprizeproj2.mapper.DepartmentMapper;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.repository.DepartmentRepository;
import org.sample.simpleenterprizeproj2.specification.DepartmentSpecification;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final AsyncNotificationService asyncNotificationService;

    public DepartmentService(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper,
                             AsyncNotificationService asyncNotificationService) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
        this.asyncNotificationService = asyncNotificationService;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "findAllFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    public Page<DepartmentResponse> findAll(String name, Pageable pageable) {
        return departmentRepository.findAll(DepartmentSpecification.build(name), pageable)
                .map(departmentMapper::toResponse);
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "findResponseByIdFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    public DepartmentResponse findResponseById(Long id) {
        return departmentMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "findEntityByIdFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    public Department findEntityById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.not.found.department", id));
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "createFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = departmentMapper.toEntity(request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Created department id={}", response.getId());
        asyncNotificationService.notifyResourceCreated("Department", response.getId());
        return response;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "updateFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findEntityById(id);
        departmentMapper.updateEntity(department, request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Updated department id={}", id);
        asyncNotificationService.notifyResourceUpdated("Department", id);
        return response;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "patchFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public DepartmentResponse patch(Long id, DepartmentPatchRequest request) {
        Department department = findEntityById(id);
        departmentMapper.patchEntity(department, request);
        DepartmentResponse response = departmentMapper.toResponse(departmentRepository.save(department));
        log.info("Patched department id={}", id);
        asyncNotificationService.notifyResourceUpdated("Department", id);
        return response;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "deleteFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public void delete(Long id) {
        Department department = findEntityById(id);
        department.setDeleted(true);
        departmentRepository.save(department);
        log.info("Soft-deleted department id={}", id);
        asyncNotificationService.notifyResourceDeleted("Department", id);
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "bulkCreateFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public List<DepartmentResponse> bulkCreate(List<DepartmentRequest> requests) {
        List<Department> entities = new ArrayList<>(requests.size());
        for (DepartmentRequest request : requests) {
            entities.add(departmentMapper.toEntity(request));
        }
        List<Department> saved = departmentRepository.saveAll(entities);
        List<DepartmentResponse> responses = new ArrayList<>(saved.size());
        for (Department department : saved) {
            responses.add(departmentMapper.toResponse(department));
            log.info("Created department id={}", department.getId());
            asyncNotificationService.notifyResourceCreated("Department", department.getId());
        }
        return responses;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "bulkUpdateFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public List<DepartmentResponse> bulkUpdate(List<BulkUpdateRequest<DepartmentRequest>> requests) {
        List<Department> entities = new ArrayList<>(requests.size());
        for (BulkUpdateRequest<DepartmentRequest> request : requests) {
            Department department = findEntityById(request.getId());
            departmentMapper.updateEntity(department, request.getData());
            entities.add(department);
        }
        List<Department> saved = departmentRepository.saveAll(entities);
        List<DepartmentResponse> responses = new ArrayList<>(saved.size());
        for (Department department : saved) {
            responses.add(departmentMapper.toResponse(department));
            log.info("Updated department id={}", department.getId());
            asyncNotificationService.notifyResourceUpdated("Department", department.getId());
        }
        return responses;
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "bulkDeleteFallback")
    @Bulkhead(name = "departmentService")
    @Retry(name = "departmentService")
    @Transactional
    public void bulkDelete(List<Long> ids) {
        List<Department> entities = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Department department = findEntityById(id);
            department.setDeleted(true);
            entities.add(department);
        }
        departmentRepository.saveAll(entities);
        for (Department department : entities) {
            log.info("Soft-deleted department id={}", department.getId());
            asyncNotificationService.notifyResourceDeleted("Department", department.getId());
        }
    }

    // ── Fallback methods ──────────────────────────────────────────────

    private Page<DepartmentResponse> findAllFallback(String name, Pageable pageable, Throwable t) {
        log.warn("Fallback for findAll triggered: {}", t.getMessage());
        return Page.empty(pageable);
    }

    private DepartmentResponse findResponseByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findResponseById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private Department findEntityByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findEntityById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private DepartmentResponse createFallback(DepartmentRequest request, Throwable t) {
        log.warn("Fallback for create triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private DepartmentResponse updateFallback(Long id, DepartmentRequest request, Throwable t) {
        log.warn("Fallback for update(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private DepartmentResponse patchFallback(Long id, DepartmentPatchRequest request, Throwable t) {
        log.warn("Fallback for patch(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private void deleteFallback(Long id, Throwable t) {
        log.warn("Fallback for delete(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private List<DepartmentResponse> bulkCreateFallback(List<DepartmentRequest> requests, Throwable t) {
        log.warn("Fallback for bulkCreate triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private List<DepartmentResponse> bulkUpdateFallback(List<BulkUpdateRequest<DepartmentRequest>> requests, Throwable t) {
        log.warn("Fallback for bulkUpdate triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }

    private void bulkDeleteFallback(List<Long> ids, Throwable t) {
        log.warn("Fallback for bulkDelete triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Department service is temporarily unavailable", t);
    }
}
