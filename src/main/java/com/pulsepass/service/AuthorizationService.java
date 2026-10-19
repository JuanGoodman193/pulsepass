package com.pulsepass.service;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthorizationService {

    public Optional<SecurityPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public SecurityPrincipal requireCurrentPrincipal() {
        return getCurrentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No hay usuario autenticado en el contexto"));
    }

    public boolean isStaffOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Valida que una consulta de usuario pertenezca al propio usuario autenticado o a un ADMIN.
     */
    public void checkUserAccess(User targetUser) {
        if (isAdmin()) {
            return;
        }
        SecurityPrincipal principal = requireCurrentPrincipal();
        if (!principal.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException("No tiene permisos para consultar la información de otro usuario");
        }
    }

    /**
     * Valida que la compra de un ticket pertenezca al usuario autenticado (para rol USER).
     */
    public void checkTicketPurchase(String userEmail) {
        if (isAdmin()) {
            return;
        }
        SecurityPrincipal principal = requireCurrentPrincipal();
        if (!principal.getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Un usuario solo puede comprar entradas para sí mismo");
        }
    }

    /**
     * Valida que la consulta de un ticket pertenezca al propietario o a STAFF / ADMIN.
     */
    public void checkTicketAccess(Ticket ticket) {
        if (isStaffOrAdmin()) {
            return;
        }
        SecurityPrincipal principal = requireCurrentPrincipal();
        if (!ticket.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No tiene permisos para consultar este ticket");
        }
    }

    /**
     * Valida que la consulta de tickets por correo pertenezca al usuario autenticado o a ADMIN.
     */
    public void checkTicketsByUserEmail(String email) {
        if (isAdmin()) {
            return;
        }
        SecurityPrincipal principal = requireCurrentPrincipal();
        if (!principal.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No tiene permisos para consultar tickets de otro usuario");
        }
    }

    /**
     * Valida que la cancelación de un ticket sea efectuada por su propietario (USER) o por un ADMIN.
     */
    public void checkTicketCancel(Ticket ticket) {
        if (isAdmin()) {
            return;
        }
        SecurityPrincipal principal = requireCurrentPrincipal();
        if (!ticket.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No tiene permisos para cancelar un ticket de otro usuario");
        }
    }
}
