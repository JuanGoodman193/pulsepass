package com.pulsepass.security;

import com.pulsepass.controller.ArtistController;
import com.pulsepass.controller.AuthController;
import com.pulsepass.controller.EventController;
import com.pulsepass.controller.TicketController;
import com.pulsepass.controller.UserController;
import com.pulsepass.controller.VenueController;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        EventController.class,
        TicketController.class,
        UserController.class,
        AuthController.class,
        VenueController.class,
        ArtistController.class
})
@Import({
        SecurityConfig.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class SecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private SecurityPrincipal userPrincipal;
    private SecurityPrincipal staffPrincipal;
    private SecurityPrincipal adminPrincipal;
    private SecurityPrincipal inactivePrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new SecurityPrincipal(
                1L, "andrea", "andrea@example.com", "$2a$10$hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        staffPrincipal = new SecurityPrincipal(
                2L, "laura_staff", "laura@pulsepass.com", "$2a$10$hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
        adminPrincipal = new SecurityPrincipal(
                3L, "admin_user", "admin@pulsepass.com", "$2a$10$hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        inactivePrincipal = new SecurityPrincipal(
                4L, "disabled_user", "disabled@example.com", "$2a$10$hash", false,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(userDetailsService.loadUserById(1L)).thenReturn(userPrincipal);
        when(userDetailsService.loadUserById(2L)).thenReturn(staffPrincipal);
        when(userDetailsService.loadUserById(3L)).thenReturn(adminPrincipal);
        when(userDetailsService.loadUserById(4L)).thenReturn(inactivePrincipal);
    }

    private String createBearerHeader(Long userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    // =========================================================================
    // T-SEC-001: Anónimo GET /api/events/published -> 200
    // =========================================================================
    @Test
    @DisplayName("T-SEC-001: Anónimo GET /api/events/published -> 200")
    void tSec001_anonymousCanAccessPublishedEvents() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L, "CMF-2026", "Caribbean Music Fest", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), "Marina Center"
        );
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findPublishedEvents();
    }

    // =========================================================================
    // T-SEC-002: Anónimo POST /api/events -> 401
    // =========================================================================
    @Test
    @DisplayName("T-SEC-002: Anónimo POST /api/events -> 401")
    void tSec002_anonymousCannotCreateEvent() throws Exception {
        String json = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Festival",
                  "description": "Desc",
                  "category": "MUSIC",
                  "eventDate": "2026-11-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(eventService, never()).create(any());
    }

    // =========================================================================
    // T-SEC-003: USER POST /api/events -> 403, sin invocar Service
    // =========================================================================
    @Test
    @DisplayName("T-SEC-003: USER POST /api/events -> 403, sin invocar Service")
    void tSec003_userCannotCreateEvent() throws Exception {
        String json = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Festival",
                  "description": "Desc",
                  "category": "MUSIC",
                  "eventDate": "2026-11-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .header("Authorization", createBearerHeader(1L)) // USER
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));

        verify(eventService, never()).create(any());
    }

    // =========================================================================
    // T-SEC-004: ADMIN POST /api/events válido -> 201
    // =========================================================================
    @Test
    @DisplayName("T-SEC-004: ADMIN POST /api/events válido -> 201")
    void tSec004_adminCanCreateEvent() throws Exception {
        String json = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Festival",
                  "description": "Desc",
                  "category": "MUSIC",
                  "eventDate": "2026-11-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Festival", "Desc", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(20), 18, "VEN-SMR-01", "Marina Center", Collections.emptySet()
        );
        when(eventService.create(any(CreateEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", createBearerHeader(3L)) // ADMIN
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    // =========================================================================
    // T-SEC-005: Anónimo POST /api/auth/login correcto -> 200 con Bearer
    // =========================================================================
    @Test
    @DisplayName("T-SEC-005: Anónimo POST /api/auth/login correcto -> 200 con Bearer")
    void tSec005_anonymousLoginSuccessfullyReturnsToken() throws Exception {
        String json = """
                {
                  "identifier": "andrea",
                  "password": "PasswordSeguro123!"
                }
                """;

        AuthResponse authResponse = new AuthResponse("mock.jwt.token", "Bearer", 900L);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(authService).login(any(LoginRequest.class));
    }

    // =========================================================================
    // T-SEC-006: Login con contraseña incorrecta -> 401 genérico
    // =========================================================================
    @Test
    @DisplayName("T-SEC-006: Login con contraseña incorrecta -> 401 genérico")
    void tSec006_loginWithWrongPasswordReturns401() throws Exception {
        String json = """
                {
                  "identifier": "andrea",
                  "password": "PasswordErroneo!"
                }
                """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    // =========================================================================
    // T-SEC-007: Token expirado en ruta protegida -> 401 con ErrorResponse
    // =========================================================================
    @Test
    @DisplayName("T-SEC-007: Token expirado en ruta protegida -> 401 con ErrorResponse")
    void tSec007_expiredTokenReturns401WithErrorResponse() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretBase64()));
        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                .subject("1")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now.minusSeconds(100)))
                .expiration(Date.from(now.minusSeconds(10)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("JWT token is invalid, expired, or tampered"));
    }

    // =========================================================================
    // T-SEC-008: Token con firma alterada -> 401, sin invocar Service
    // =========================================================================
    @Test
    @DisplayName("T-SEC-008: Token con firma alterada -> 401, sin invocar Service")
    void tSec008_tamperedTokenReturns401WithoutServiceInvocation() throws Exception {
        String token = jwtService.generateToken(1L);
        char lastChar = token.charAt(token.length() - 1);
        char altered = (lastChar == 'x') ? 'y' : 'x';
        String tampered = token.substring(0, token.length() - 1) + altered;

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, never()).getMe();
    }

    // =========================================================================
    // T-SEC-009: USER consulta ticket ajeno -> 403
    // =========================================================================
    @Test
    @DisplayName("T-SEC-009: USER consulta ticket ajeno -> 403")
    void tSec009_userAccessingThirdPartyTicketReturns403() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS-01"))
                .thenThrow(new AccessDeniedException("No tiene permisos para consultar este ticket"));

        mockMvc.perform(get("/api/tickets/TCK-CARLOS-01")
                        .header("Authorization", createBearerHeader(1L))) // Andrea (USER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para consultar este ticket"));
    }

    // =========================================================================
    // T-SEC-010: USER compra con email ajeno -> 403; 0 tickets creados
    // =========================================================================
    @Test
    @DisplayName("T-SEC-010: USER compra con email ajeno -> 403; 0 tickets creados")
    void tSec010_userPurchasingForAnotherUserReturns403() throws Exception {
        String json = """
                {
                  "userEmail": "carlos@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new AccessDeniedException("Un usuario solo puede comprar entradas para sí mismo"));

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", createBearerHeader(1L)) // Andrea
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Un usuario solo puede comprar entradas para sí mismo"));
    }

    // =========================================================================
    // T-SEC-011: USER cancela ticket propio -> 200 si reglas del Service lo permiten
    // =========================================================================
    @Test
    @DisplayName("T-SEC-011: USER cancela ticket propio -> 200")
    void tSec011_userCancelsOwnTicketSuccessfully() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-ANDREA-01", TicketType.GENERAL, BigDecimal.valueOf(50),
                TicketStatus.CANCELLED, LocalDateTime.now().minusDays(1), "andrea@example.com", "CMF-2026", "Festival"
        );
        when(ticketService.cancel("TCK-ANDREA-01")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-ANDREA-01/cancel")
                        .header("Authorization", createBearerHeader(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-ANDREA-01"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-ANDREA-01");
    }

    // =========================================================================
    // T-SEC-012: USER intenta usar ticket -> 403
    // =========================================================================
    @Test
    @DisplayName("T-SEC-012: USER intenta usar ticket -> 403")
    void tSec012_userCannotUseTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/TCK-ANDREA-01/use")
                        .header("Authorization", createBearerHeader(1L))) // USER
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(ticketService, never()).markAsUsed(any());
    }

    // =========================================================================
    // T-SEC-013: STAFF marca ticket válido como usado -> 200
    // =========================================================================
    @Test
    @DisplayName("T-SEC-013: STAFF marca ticket válido como usado -> 200")
    void tSec013_staffCanMarkTicketAsUsed() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-ANDREA-01", TicketType.GENERAL, BigDecimal.valueOf(50),
                TicketStatus.USED, LocalDateTime.now().minusDays(1), "andrea@example.com", "CMF-2026", "Festival"
        );
        when(ticketService.markAsUsed("TCK-ANDREA-01")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-ANDREA-01/use")
                        .header("Authorization", createBearerHeader(2L))) // STAFF
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-ANDREA-01");
    }

    // =========================================================================
    // T-SEC-014: ADMIN publica evento -> 200
    // =========================================================================
    @Test
    @DisplayName("T-SEC-014: ADMIN publica evento -> 200")
    void tSec014_adminCanPublishEvent() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Festival", "Desc", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(20), 18, "VEN-SMR-01", "Marina Center", Collections.emptySet()
        );
        when(eventService.publish("CMF-2026")).thenReturn(response);

        mockMvc.perform(patch("/api/events/CMF-2026/publish")
                        .header("Authorization", createBearerHeader(3L))) // ADMIN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    // =========================================================================
    // T-SEC-015: USER busca datos de otro usuario -> 403
    // =========================================================================
    @Test
    @DisplayName("T-SEC-015: USER busca datos de otro usuario -> 403")
    void tSec015_userSearchingAnotherUserReturns403() throws Exception {
        when(userService.findByEmail("carlos@example.com"))
                .thenThrow(new AccessDeniedException("No tiene permisos para consultar la información de otro usuario"));

        mockMvc.perform(get("/api/users/by-email")
                        .header("Authorization", createBearerHeader(1L)) // Andrea
                        .param("email", "carlos@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // =========================================================================
    // T-SEC-016: Un token válido pertenece a usuario inactivo -> 401
    // =========================================================================
    @Test
    @DisplayName("T-SEC-016: Un token válido pertenece a usuario inactivo -> 401")
    void tSec016_validTokenForDisabledUserReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", createBearerHeader(4L))) // Usuario inactivo
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, never()).getMe();
    }

    // =========================================================================
    // T-SEC-017: Anónimo consulta evento no publicado -> No expone datos del borrador (404)
    // =========================================================================
    @Test
    @DisplayName("T-SEC-017: Anónimo consulta evento no publicado -> No expone datos del borrador (404)")
    void tSec017_anonymousAccessingDraftEventReturns404() throws Exception {
        when(eventService.findByCode("DRAFT-EVENT"))
                .thenThrow(new ResourceNotFoundException("Event not found: DRAFT-EVENT"));

        mockMvc.perform(get("/api/events/DRAFT-EVENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // =========================================================================
    // T-SEC-018: Request inválido pero autorizado -> 400 previo de Bean Validation
    // =========================================================================
    @Test
    @DisplayName("T-SEC-018: Request inválido pero autorizado -> 400 previo de Bean Validation")
    void tSec018_invalidRequestFailsValidationBeforeService() throws Exception {
        // Enviar request con campos vacíos siendo ADMIN
        String invalidJson = """
                {
                  "eventCode": "",
                  "name": "",
                  "description": "",
                  "category": null,
                  "eventDate": null,
                  "minimumAge": -1,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(post("/api/events")
                        .header("Authorization", createBearerHeader(3L)) // ADMIN
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(eventService, never()).create(any());
    }

    // =========================================================================
    // T-SEC-019: Login o JWT da error -> No devuelve stacktrace, password ni hash
    // =========================================================================
    @Test
    @DisplayName("T-SEC-019: Login o JWT da error -> No devuelve stacktrace, password ni hash")
    void tSec019_errorsDoNotLeakSensitiveInformation() throws Exception {
        String json = """
                {
                  "identifier": "andrea",
                  "password": "wrong"
                }
                """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("hash"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at com.pulsepass"))));
    }

    // =========================================================================
    // T-SEC-020: Pruebas REST previas con identidad apropiada -> Mantienen códigos y DTOs exitosos
    // =========================================================================
    @Test
    @DisplayName("T-SEC-020: Pruebas REST previas con identidad apropiada -> Mantienen códigos y DTOs exitosos")
    void tSec020_authorizedUserCanPurchaseTicketAndReceiveProperContract() throws Exception {
        String json = """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        TicketResponse response = new TicketResponse(
                10L, "TCK-ANDREA-10", TicketType.GENERAL, BigDecimal.valueOf(50),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@example.com", "CMF-2026", "Caribbean Music Fest"
        );
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", createBearerHeader(1L)) // USER
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("TCK-ANDREA-10"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.price").value(50.0))
                .andExpect(jsonPath("$.userEmail").value("andrea@example.com"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }
}
