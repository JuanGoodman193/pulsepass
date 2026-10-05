package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper mapper;

    @InjectMocks
    private EventServiceImpl service;

    @Test
    @DisplayName("TEST-EVENT-001: Evento existente -> retorna DTO")
    void shouldFindEventByCodeWhenExists() {
        // ARRANGE
        Event event = createSampleEvent("CMF-2026", EventStatus.DRAFT);
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest", "Desc",
                EventCategory.MUSIC, EventStatus.DRAFT, event.getEventDate(),
                18, "VEN-01", "Marina Center", Collections.emptySet()
        );

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(response);

        // ACT
        EventResponse result = service.findByCode("CMF-2026");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.eventCode()).isEqualTo("CMF-2026");
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(mapper).toResponse(event);
    }

    @Test
    @DisplayName("TEST-EVENT-002: Evento inexistente -> ResourceNotFoundException")
    void shouldThrowExceptionWhenEventDoesNotExist() {
        // ARRANGE
        when(eventRepository.findByEventCode("NOT-FOUND")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.findByCode("NOT-FOUND"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(eventRepository).findByEventCode("NOT-FOUND");
        verify(mapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("TEST-EVENT-003: Crear evento válido -> save() ejecutado")
    void shouldCreateEventSuccessfully() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(30), 18, "VEN-01"
        );
        Venue venue = new Venue("VEN-01", "Marina Center", "Santa Marta", "Address", 100, true);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest", "Desc",
                EventCategory.MUSIC, EventStatus.DRAFT, request.eventDate(),
                18, "VEN-01", "Marina Center", Collections.emptySet()
        );
        when(mapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = service.create(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("TEST-EVENT-004: Venue inexistente -> error y save() nunca ejecutado")
    void shouldThrowExceptionWhenVenueDoesNotExistOnCreation() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Name", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(10), 18, "UNKNOWN-VEN"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("UNKNOWN-VEN")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-005: Venue inactivo -> BusinessRuleException")
    void shouldThrowExceptionWhenVenueIsInactiveOnCreation() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Name", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(10), 18, "VEN-01"
        );
        Venue venue = new Venue("VEN-01", "Marina Center", "Santa Marta", "Address", 100, false);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-01")).thenReturn(Optional.of(venue));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive venue");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-006: Fecha pasada -> BusinessRuleException")
    void shouldThrowExceptionWhenEventDateIsInThePast() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Name", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().minusDays(1), 18, "VEN-01"
        );
        Venue venue = new Venue("VEN-01", "Marina Center", "Santa Marta", "Address", 100, true);

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-01")).thenReturn(Optional.of(venue));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-EVENT-007: Publicar DRAFT válido -> PUBLISHED")
    void shouldPublishDraftEvent() {
        // ARRANGE
        Event event = createSampleEvent("CMF-2026", EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest", "Desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, event.getEventDate(),
                18, "VEN-01", "Marina Center", Collections.emptySet()
        );
        when(mapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = service.publish("CMF-2026");

        // ASSERT
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("TEST-EVENT-008: Publicar CANCELLED -> BusinessRuleException y no persistir")
    void shouldThrowExceptionWhenPublishingCancelledEvent() {
        // ARRANGE
        Event event = createSampleEvent("CMF-2026", EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only DRAFT events can be published");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asociar artista a evento exitosamente")
    void shouldAddArtistToEvent() {
        // ARRANGE
        Event event = createSampleEvent("CMF-2026", EventStatus.DRAFT);
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Name", "Desc",
                EventCategory.MUSIC, EventStatus.DRAFT, event.getEventDate(),
                18, "VEN-01", "Center", Collections.singleton("Solar Beat")
        );
        when(mapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = service.addArtist("CMF-2026", 1L);

        // ASSERT
        assertThat(result.artists()).contains("Solar Beat");
        verify(eventRepository).save(event);
    }

    private Event createSampleEvent(String code, EventStatus status) {
        Venue venue = new Venue("VEN-01", "Marina Center", "Santa Marta", "Address", 100, true);
        Event event = new Event(
                code, "Caribbean Music Fest", "Desc",
                EventCategory.MUSIC, status, LocalDateTime.now().plusMonths(2), 18, venue
        );
        return event;
    }
}
