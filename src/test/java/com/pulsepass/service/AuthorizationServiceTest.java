package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String username, String email, String role) {
        SecurityPrincipal principal = new SecurityPrincipal(
                userId,
                username,
                email,
                "$2a$10$hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    @DisplayName("Dueño consulta su propia información -> permitido")
    void shouldAllowOwnerAccess() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");
        User user = new User("andrea", "andrea@example.com", true);
        ReflectionTestUtils.setField(user, "id", 1L);

        assertThatCode(() -> authorizationService.checkUserAccess(user))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("USER consulta información de otro usuario -> AccessDeniedException (403)")
    void shouldDenyThirdPartyAccess() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");
        User carlos = new User("carlos", "carlos@example.com", true);
        ReflectionTestUtils.setField(carlos, "id", 2L);

        assertThatThrownBy(() -> authorizationService.checkUserAccess(carlos))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permisos para consultar la información de otro usuario");
    }

    @Test
    @DisplayName("ADMIN consulta información de cualquier usuario -> permitido")
    void shouldAllowAdminAccessToAnyUser() {
        authenticate(99L, "admin", "admin@pulsepass.com", "ADMIN");
        User carlos = new User("carlos", "carlos@example.com", true);
        ReflectionTestUtils.setField(carlos, "id", 2L);

        assertThatCode(() -> authorizationService.checkUserAccess(carlos))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("USER compra ticket con su propio email -> permitido")
    void shouldAllowOwnTicketPurchase() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");

        assertThatCode(() -> authorizationService.checkTicketPurchase("andrea@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("USER compra ticket con email ajeno -> AccessDeniedException (403)")
    void shouldDenyPurchaseWithThirdPartyEmail() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");

        assertThatThrownBy(() -> authorizationService.checkTicketPurchase("carlos@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Un usuario solo puede comprar entradas para sí mismo");
    }

    @Test
    @DisplayName("USER consulta su propio ticket -> permitido")
    void shouldAllowOwnerTicketAccess() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");
        User andrea = new User("andrea", "andrea@example.com", true);
        ReflectionTestUtils.setField(andrea, "id", 1L);

        Ticket ticket = new Ticket("TCK-001", TicketType.GENERAL, BigDecimal.valueOf(50), TicketStatus.PAID, LocalDateTime.now(), andrea, null);

        assertThatCode(() -> authorizationService.checkTicketAccess(ticket))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("USER consulta ticket de otro usuario -> AccessDeniedException (403)")
    void shouldDenyThirdPartyTicketAccess() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");
        User carlos = new User("carlos", "carlos@example.com", true);
        ReflectionTestUtils.setField(carlos, "id", 2L);

        Ticket ticket = new Ticket("TCK-002", TicketType.GENERAL, BigDecimal.valueOf(50), TicketStatus.PAID, LocalDateTime.now(), carlos, null);

        assertThatThrownBy(() -> authorizationService.checkTicketAccess(ticket))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("STAFF y ADMIN pueden consultar tickets ajenos")
    void shouldAllowStaffAndAdminTicketAccess() {
        User carlos = new User("carlos", "carlos@example.com", true);
        ReflectionTestUtils.setField(carlos, "id", 2L);
        Ticket ticket = new Ticket("TCK-002", TicketType.GENERAL, BigDecimal.valueOf(50), TicketStatus.PAID, LocalDateTime.now(), carlos, null);

        authenticate(50L, "staff1", "staff@pulsepass.com", "STAFF");
        assertThatCode(() -> authorizationService.checkTicketAccess(ticket))
                .doesNotThrowAnyException();

        authenticate(99L, "admin", "admin@pulsepass.com", "ADMIN");
        assertThatCode(() -> authorizationService.checkTicketAccess(ticket))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("USER cancela ticket propio -> permitido; ticket ajeno -> denegado")
    void shouldValidateTicketCancelOwnership() {
        authenticate(1L, "andrea", "andrea@example.com", "USER");
        User andrea = new User("andrea", "andrea@example.com", true);
        ReflectionTestUtils.setField(andrea, "id", 1L);
        User carlos = new User("carlos", "carlos@example.com", true);
        ReflectionTestUtils.setField(carlos, "id", 2L);

        Ticket ownTicket = new Ticket("TCK-001", TicketType.GENERAL, BigDecimal.valueOf(50), TicketStatus.PAID, LocalDateTime.now(), andrea, null);
        Ticket otherTicket = new Ticket("TCK-002", TicketType.GENERAL, BigDecimal.valueOf(50), TicketStatus.PAID, LocalDateTime.now(), carlos, null);

        assertThatCode(() -> authorizationService.checkTicketCancel(ownTicket))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> authorizationService.checkTicketCancel(otherTicket))
                .isInstanceOf(AccessDeniedException.class);
    }
}
