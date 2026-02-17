package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.UserPatchRequest;
import org.sample.simpleenterprizeproj2.dto.UserRequest;
import org.sample.simpleenterprizeproj2.dto.UserResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.exception.ServiceUnavailableException;
import org.sample.simpleenterprizeproj2.mapper.UserMapper;
import org.sample.simpleenterprizeproj2.model.User;
import org.sample.simpleenterprizeproj2.repository.UserRepository;
import org.sample.simpleenterprizeproj2.specification.UserSpecification;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String CACHE_NAME = "users";
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AsyncNotificationService asyncNotificationService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       AsyncNotificationService asyncNotificationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.asyncNotificationService = asyncNotificationService;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "findAllFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    public Page<UserResponse> findAll(String username, String email, String role, Pageable pageable) {
        return userRepository.findAll(UserSpecification.build(username, email, role), pageable)
                .map(userMapper::toResponse);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "findResponseByIdFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    @Cacheable(value = CACHE_NAME, key = "#id")
    public UserResponse findResponseById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "findEntityByIdFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "createFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#result.id")
    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Created user id={}", response.getId());
        asyncNotificationService.notifyResourceCreated("User", response.getId());
        return response;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "updateFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        userMapper.updateEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Updated user id={}", id);
        asyncNotificationService.notifyResourceUpdated("User", id);
        return response;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "patchFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse patch(Long id, UserPatchRequest request) {
        User user = findEntityById(id);
        userMapper.patchEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Patched user id={}", id);
        asyncNotificationService.notifyResourceUpdated("User", id);
        return response;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "deleteFallback")
    @Bulkhead(name = "userService")
    @Retry(name = "userService")
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        User user = findEntityById(id);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Soft-deleted user id={}", id);
        asyncNotificationService.notifyResourceDeleted("User", id);
    }

    // ── Fallback methods ──────────────────────────────────────────────

    private Page<UserResponse> findAllFallback(String username, String email, String role,
                                               Pageable pageable, Throwable t) {
        log.warn("Fallback for findAll triggered: {}", t.getMessage());
        return Page.empty(pageable);
    }

    private UserResponse findResponseByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findResponseById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }

    private User findEntityByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findEntityById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }

    private UserResponse createFallback(UserRequest request, Throwable t) {
        log.warn("Fallback for create triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }

    private UserResponse updateFallback(Long id, UserRequest request, Throwable t) {
        log.warn("Fallback for update(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }

    private UserResponse patchFallback(Long id, UserPatchRequest request, Throwable t) {
        log.warn("Fallback for patch(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }

    private void deleteFallback(Long id, Throwable t) {
        log.warn("Fallback for delete(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("User service is temporarily unavailable", t);
    }
}
