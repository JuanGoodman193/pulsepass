package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private SecurityPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new SecurityPrincipal(
                1L,
                "andrea",
                "andrea@example.com",
                "$2a$10$hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Login exitoso emite JWT y metadatos correctos")
    void shouldLoginSuccessfullyAndReturnAuthResponse() {
        LoginRequest request = new LoginRequest("andrea", "PasswordSeguro123!");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateToken(1L)).thenReturn("mock.jwt.token");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(1L);
    }

    @Test
    @DisplayName("Login con contraseña incorrecta arroja BadCredentialsException genérico (401)")
    void shouldThrowBadCredentialsWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest("andrea", "PasswordErroneo!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Login con usuario inactivo arroja BadCredentialsException genérico (401)")
    void shouldThrowBadCredentialsWhenUserIsDisabled() {
        LoginRequest request = new LoginRequest("andrea", "PasswordSeguro123!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas o cuenta deshabilitada");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("getMe retorna información mínima del usuario autenticado sin secretos")
    void shouldReturnAuthenticatedUserInfo() {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthenticatedUserResponse response = authService.getMe();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("andrea");
        assertThat(response.email()).isEqualTo("andrea@example.com");
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("getMe sin usuario autenticado arroja BadCredentialsException")
    void shouldThrowWhenNoUserAuthenticatedInGetMe() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.getMe())
                .isInstanceOf(BadCredentialsException.class);
    }
}
