package com.pulsepass.service.impl;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final UserMapper mapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            AuthorizationService authorizationService,
            UserMapper mapper) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
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

        // Validación de contraseña segura
        if (request.password() == null || request.password().length() < 12) {
            throw new BusinessRuleException("Password must have at least 12 characters");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessRuleException("Password cannot exceed 72 bytes");
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

        User savedUser = userRepository.save(user);

        // Registro atómico de credenciales (BCrypt hash)
        String hash = passwordEncoder.encode(request.password());
        UserCredential credential = new UserCredential(savedUser, hash);
        userCredentialRepository.save(credential);

        // Asignación atómica del rol inicial USER (nunca STAFF ni ADMIN en registro público)
        UserRole userRole = new UserRole(savedUser, Role.USER);
        userRoleRepository.save(userRole);

        return mapper.toResponse(savedUser);
    }

    @Override
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Control de propiedad / privacidad
        if (authorizationService != null) {
            authorizationService.checkUserAccess(user);
        }

        return mapper.toResponse(user);
    }

    @Override
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Control de propiedad / privacidad
        if (authorizationService != null) {
            authorizationService.checkUserAccess(user);
        }

        return mapper.toResponse(user);
    }
}
