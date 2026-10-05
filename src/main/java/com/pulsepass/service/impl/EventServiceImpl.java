package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper mapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            ArtistRepository artistRepository,
            EventMapper mapper) {

        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: Código único
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002: Venue obligatorio
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        // BR-EVENT-003: Venue activo
        if (!venue.isActive()) {
            throw new BusinessRuleException("Cannot create event in an inactive venue: " + request.venueCode());
        }

        // BR-EVENT-004: Fecha válida (futura)
        if (request.eventDate() == null || request.eventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future");
        }

        // BR-EVENT-006: Edad mínima >= 0
        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age must be greater than or equal to 0");
        }

        // BR-EVENT-005: Estado inicial DRAFT
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge(),
                venue
        );

        Event saved = eventRepository.save(event);
        return mapper.toResponse(saved);
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        // BR-EVENT-007: Solo se puede publicar un evento en estado DRAFT
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published. Current status: " + event.getStatus());
        }

        // BR-EVENT-008: El evento debe seguir teniendo fecha futura
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish event with a past date: " + event.getEventDate());
        }

        // BR-EVENT-009: El venue debe continuar activo
        if (event.getVenue() == null || !event.getVenue().isActive()) {
            throw new BusinessRuleException("Cannot publish event because venue is inactive");
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        // BR-EVENT-011: No se pueden agregar artistas a eventos CANCELLED o FINISHED
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot add artists to a cancelled or finished event");
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistId));

        // BR-EVENT-010: No se puede asociar dos veces el mismo artista al evento
        boolean alreadyAssociated = event.getArtists().stream()
                .anyMatch(a -> a.getId() != null && a.getId().equals(artistId)
                        || a.getStageName().equalsIgnoreCase(artist.getStageName()));
        if (alreadyAssociated) {
            throw new BusinessRuleException("Artist is already associated with this event: " + artist.getStageName());
        }

        event.addArtist(artist);
        Event saved = eventRepository.save(event);
        return mapper.toResponse(saved);
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }
}
