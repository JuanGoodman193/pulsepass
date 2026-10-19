package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
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
import com.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    @DisplayName("TEST-USER-001: Registrar usuario válido")
    void shouldRegisterValidUser() {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea25", "andrea@email.com", "PasswordSeguro123!", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2001, 5, 20)
        );

        when(userRepository.existsByUsername("andrea25")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(passwordEncoder.encode("PasswordSeguro123!")).thenReturn("$2a$10$hashedpasswordxyz");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = new UserResponse(
                1L, "andrea25", "andrea@email.com", true,
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(2001, 5, 20)
        );
        when(mapper.toResponse(any(User.class))).thenReturn(response);

        // ACT
        UserResponse result = service.register(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("andrea25");
        assertThat(result.active()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(userCredentialRepository).save(any(UserCredential.class));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("TEST-USER-002: Username duplicado -> DuplicateResourceException")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea25", "andrea@email.com", "PasswordSeguro123!", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2001, 5, 20)
        );
        when(userRepository.existsByUsername("andrea25")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
        verify(userCredentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-USER-003: Email duplicado -> DuplicateResourceException")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea25", "andrea@email.com", "PasswordSeguro123!", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2001, 5, 20)
        );
        when(userRepository.existsByUsername("andrea25")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(userCredentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-USER-004: Birth date futura -> BusinessRuleException")
    void shouldThrowExceptionWhenBirthDateIsInTheFuture() {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea25", "andrea@email.com", "PasswordSeguro123!", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.now().plusDays(1)
        );
        when(userRepository.existsByUsername("andrea25")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-USER-005: Contraseña corta (< 12 caracteres) -> BusinessRuleException")
    void shouldThrowExceptionWhenPasswordIsTooShort() {
        // ARRANGE
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea25", "andrea@email.com", "corta", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2001, 5, 20)
        );
        when(userRepository.existsByUsername("andrea25")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("12 characters");

        verify(userRepository, never()).save(any());
        verify(userCredentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Buscar usuario por email existente")
    void shouldFindUserByEmail() {
        // ARRANGE
        User user = new User("andrea25", "andrea@email.com", true);
        UserResponse response = new UserResponse(
                1L, "andrea25", "andrea@email.com", true,
                "Andrea", "Gomez", null, null, LocalDate.of(2001, 5, 20)
        );
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(response);

        // ACT
        UserResponse result = service.findByEmail("andrea@email.com");

        // ASSERT
        assertThat(result.email()).isEqualTo("andrea@email.com");
        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(authorizationService).checkUserAccess(user);
    }

    @Test
    @DisplayName("Buscar usuario inexistente lanza ResourceNotFoundException")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("unknown@email.com")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.findByEmail("unknown@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
