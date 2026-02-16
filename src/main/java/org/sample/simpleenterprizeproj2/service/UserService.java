package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.dto.UserPatchRequest;
import org.sample.simpleenterprizeproj2.dto.UserRequest;
import org.sample.simpleenterprizeproj2.dto.UserResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.mapper.UserMapper;
import org.sample.simpleenterprizeproj2.model.User;
import org.sample.simpleenterprizeproj2.repository.UserRepository;
import org.sample.simpleenterprizeproj2.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public Page<UserResponse> findAll(String username, String email, String role, Pageable pageable) {
        return userRepository.findAll(UserSpecification.build(username, email, role), pageable)
                .map(userMapper::toResponse);
    }

    public UserResponse findResponseById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        userMapper.updateEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse patch(Long id, UserPatchRequest request) {
        User user = findEntityById(id);
        userMapper.patchEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findEntityById(id);
        user.setDeleted(true);
        userRepository.save(user);
    }
}
