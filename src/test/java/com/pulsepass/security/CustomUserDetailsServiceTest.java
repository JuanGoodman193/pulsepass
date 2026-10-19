package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Cargar usuario existente por username")
    void shouldLoadUserByUsernameSuccessfully() {
        User user = new User("andrea", "andrea@example.com", true);
        UserCredential credential = new UserCredential(user, "$2a$10$hashedpw");
        UserRole userRole = new UserRole(user, Role.USER);

        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of(userRole));

        UserDetails userDetails = userDetailsService.loadUserByUsername("andrea");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("andrea");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedpw");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Cargar usuario existente por email")
    void shouldLoadUserByEmailSuccessfully() {
        User user = new User("andrea", "andrea@example.com", true);
        UserCredential credential = new UserCredential(user, "$2a$10$hashedpw");
        UserRole userRole = new UserRole(user, Role.USER);

        when(userRepository.findByUsername("andrea@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of(userRole));

        UserDetails userDetails = userDetailsService.loadUserByUsername("andrea@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("andrea");
    }

    @Test
    @DisplayName("Cargar usuario inexistente arroja UsernameNotFoundException")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Usuario legado sin credenciales arroja UsernameNotFoundException")
    void shouldThrowWhenUserHasNoCredentials() {
        User legacyUser = new User("legacy_user", "legacy@example.com", true);

        when(userRepository.findByUsername("legacy_user")).thenReturn(Optional.of(legacyUser));
        when(userCredentialRepository.findByUserId(legacyUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("legacy_user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("no credentials configured");
    }

    @Test
    @DisplayName("Usuario inactivo carga con isEnabled = false")
    void shouldLoadDisabledUser() {
        User inactiveUser = new User("inactive", "inactive@example.com", false);
        UserCredential credential = new UserCredential(inactiveUser, "$2a$10$hashedpw");

        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));
        when(userCredentialRepository.findByUserId(inactiveUser.getId())).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(inactiveUser.getId())).thenReturn(List.of(new UserRole(inactiveUser, Role.USER)));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactive");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
    }
}
