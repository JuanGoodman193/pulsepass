package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // BR-USER-001: username único
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        // BR-USER-002: email único ignorando mayúsculas/minúsculas
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        // BR-USER-005: birthDate no puede ser futura
        if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future");
        }

        // BR-USER-003: active = true
        // BR-USER-004: crear User y UserProfile dentro de la misma transacción
        User user = new User(request.username(), request.email(), true);
        UserProfile profile = new UserProfile(
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.city(),
                request.birthDate()
        );
        user.assignProfile(profile);

        User saved = userRepository.save(user);
        return mapper.toResponse(saved);
    }

    @Override
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
