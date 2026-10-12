package com.pulsepass.controller;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    @DisplayName("TEST-CTRL-EVT-001: crear válido -> 201")
    void shouldCreateEventWhenRequestIsValid() throws Exception {
        String jsonRequest = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Caribbean Music Fest 2026",
                  "description": "Festival en la playa",
                  "category": "MUSIC",
                  "eventDate": "2026-11-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival en la playa",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 11, 20, 20, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of()
        );

        when(eventService.create(any(CreateEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-002: request inválido -> 400")
    void shouldReturn400WhenCreateEventRequestIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "eventCode": "",
                  "name": "",
                  "description": "Descripción",
                  "category": null,
                  "eventDate": null,
                  "minimumAge": -5,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.category").exists())
                .andExpect(jsonPath("$.details.eventDate").exists())
                .andExpect(jsonPath("$.details.minimumAge").exists())
                .andExpect(jsonPath("$.details.venueCode").exists());

        verify(eventService, never()).create(any());
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-003: consultar existente -> 200")
    void shouldReturnEventWhenCodeExists() throws Exception {
        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival en la playa",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of("Solar Beat")
        );

        when(eventService.findByCode("CMF-2026")).thenReturn(response);

        mockMvc.perform(get("/api/events/CMF-2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-004: inexistente -> 404")
    void shouldReturn404WhenEventNotFound() throws Exception {
        when(eventService.findByCode("UNKNOWN-EVENT"))
                .thenThrow(new ResourceNotFoundException("Event not found: UNKNOWN-EVENT"));

        mockMvc.perform(get("/api/events/UNKNOWN-EVENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: UNKNOWN-EVENT"));

        verify(eventService).findByCode("UNKNOWN-EVENT");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-005: listar publicados -> 200")
    void shouldReturnPublishedEvents() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30),
                "Marina Convention Center"
        );

        when(eventService.findPublishedEvents()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/published")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-006: publicar válido -> 200")
    void shouldPublishEventSuccessfully() throws Exception {
        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival en la playa",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of()
        );

        when(eventService.publish("CMF-2026")).thenReturn(response);

        mockMvc.perform(patch("/api/events/CMF-2026/publish")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-007: publicar inválido -> 409")
    void shouldReturn409WhenPublishViolatesBusinessRule() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-008: asociar artista -> 200")
    void shouldAssociateArtistSuccessfully() throws Exception {
        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival en la playa",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of("Solar Beat")
        );

        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(response);

        mockMvc.perform(post("/api/events/CMF-2026/artists/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists[0]").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @DisplayName("TEST-CTRL-EVT-009: buscar por artista -> 200")
    void shouldFindEventsByArtist() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30),
                "Marina Convention Center"
        );

        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/by-artist")
                        .param("stageName", "Solar Beat")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    @DisplayName("AC-CTRL-008 variant: crear evento duplicado -> 409")
    void shouldReturn409WhenEventCodeAlreadyExists() throws Exception {
        String jsonRequest = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Caribbean Music Fest 2026",
                  "description": "Festival en la playa",
                  "category": "MUSIC",
                  "eventDate": "2026-11-20T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new DuplicateResourceException("Event code already exists: CMF-2026"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Event code already exists: CMF-2026"));

        verify(eventService).create(any(CreateEventRequest.class));
    }
}
