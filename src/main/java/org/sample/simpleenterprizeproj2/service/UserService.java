package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.UserPatchRequest;
import org.sample.simpleenterprizeproj2.dto.UserRequest;
import org.sample.simpleenterprizeproj2.dto.UserResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.mapper.UserMapper;
import org.sample.simpleenterprizeproj2.model.User;
import org.sample.simpleenterprizeproj2.repository.UserRepository;
import org.sample.simpleenterprizeproj2.specification.UserSpecification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @CircuitBreaker(name = "userService")
    public Page<UserResponse> findAll(String username, String email, String role, Pageable pageable) {
        return userRepository.findAll(UserSpecification.build(username, email, role), pageable)
                .map(userMapper::toResponse);
    }

    @CircuitBreaker(name = "userService")
    @Cacheable(value = CACHE_NAME, key = "#id")
    public UserResponse findResponseById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "userService")
    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    @CircuitBreaker(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#result.id")
    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Created user id={}", response.getId());
        return response;
    }

    @CircuitBreaker(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        userMapper.updateEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Updated user id={}", id);
        return response;
    }

    @CircuitBreaker(name = "userService")
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse patch(Long id, UserPatchRequest request) {
        User user = findEntityById(id);
        userMapper.patchEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Patched user id={}", id);
        return response;
    }

    @CircuitBreaker(name = "userService")
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        User user = findEntityById(id);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Soft-deleted user id={}", id);
    }
}
