package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.model.User;
import org.sample.simpleenterprizeproj2.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public User create(User user) {
        return userRepository.save(user);
    }

    public User update(Long id, User updated) {
        User user = findById(id);
        user.setUsername(updated.getUsername());
        user.setEmail(updated.getEmail());
        user.setPassword(updated.getPassword());
        user.setRole(updated.getRole());
        return userRepository.save(user);
    }

    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }
}
