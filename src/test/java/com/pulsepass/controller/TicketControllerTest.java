package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("TEST-CTRL-TKT-001: compra válida -> 201")
    void shouldPurchaseTicketSuccessfully() throws Exception {
        String jsonRequest = """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2026",
                  "type": "VIP"
                }
                """;

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.VIP,
                new BigDecimal("100.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.price").value(100.00))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@example.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-002: request inválido -> 400")
    void shouldReturn400WhenPurchaseTicketRequestIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "userEmail": "invalid-email",
                  "eventCode": "",
                  "type": null
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").exists())
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.type").exists());

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-003: usuario inexistente -> 404")
    void shouldReturn404WhenUserNotFoundForPurchase() throws Exception {
        String jsonRequest = """
                {
                  "userEmail": "unknown@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with email: unknown@example.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with email: unknown@example.com"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-004: regla de negocio -> 409")
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {
        String jsonRequest = """
                {
                  "userEmail": "laura@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet the minimum age of 18. Age at event: 17"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet the minimum age of 18. Age at event: 17"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-005: consultar ticket -> 200")
    void shouldReturnTicketByCode() throws Exception {
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.GENERAL,
                new BigDecimal("50.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.findByCode("TCK-ABC1234567")).thenReturn(response);

        mockMvc.perform(get("/api/tickets/TCK-ABC1234567")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).findByCode("TCK-ABC1234567");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-005 variant: consultar ticket inexistente -> 404")
    void shouldReturn404WhenTicketNotFound() throws Exception {
        when(ticketService.findByCode("UNKNOWN-TCK"))
                .thenThrow(new ResourceNotFoundException("Ticket not found with code: UNKNOWN-TCK"));

        mockMvc.perform(get("/api/tickets/UNKNOWN-TCK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket not found with code: UNKNOWN-TCK"));

        verify(ticketService).findByCode("UNKNOWN-TCK");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-006: tickets por usuario -> 200")
    void shouldReturnTicketsByUserEmail() throws Exception {
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.GENERAL,
                new BigDecimal("50.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.findByUserEmail("andrea@example.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tickets/by-user")
                        .param("email", "andrea@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@example.com"));

        verify(ticketService).findByUserEmail("andrea@example.com");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-007: tickets PAID por evento -> 200")
    void shouldReturnPaidTicketsByEvent() throws Exception {
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.VIP,
                new BigDecimal("100.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-008: cancelar válido -> 200")
    void shouldCancelTicketSuccessfully() throws Exception {
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.GENERAL,
                new BigDecimal("50.00"),
                TicketStatus.CANCELLED,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.cancel("TCK-ABC1234567")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-ABC1234567/cancel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-ABC1234567");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-009: cancelar inválido -> 409")
    void shouldReturn409WhenCancelViolatesBusinessRule() throws Exception {
        when(ticketService.cancel("TCK-ABC1234567"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled. Current ticket status: USED"));

        mockMvc.perform(patch("/api/tickets/TCK-ABC1234567/cancel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be cancelled. Current ticket status: USED"));

        verify(ticketService).cancel("TCK-ABC1234567");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-010: usar válido -> 200")
    void shouldMarkTicketAsUsedSuccessfully() throws Exception {
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-ABC1234567",
                TicketType.GENERAL,
                new BigDecimal("50.00"),
                TicketStatus.USED,
                LocalDateTime.now(),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.markAsUsed("TCK-ABC1234567")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-ABC1234567/use")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-ABC1234567"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-ABC1234567");
    }

    @Test
    @DisplayName("TEST-CTRL-TKT-011: usar inválido -> 409")
    void shouldReturn409WhenMarkAsUsedViolatesBusinessRule() throws Exception {
        when(ticketService.markAsUsed("TCK-ABC1234567"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be marked as used. Current ticket status: CANCELLED"));

        mockMvc.perform(patch("/api/tickets/TCK-ABC1234567/use")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be marked as used. Current ticket status: CANCELLED"));

        verify(ticketService).markAsUsed("TCK-ABC1234567");
    }
}
