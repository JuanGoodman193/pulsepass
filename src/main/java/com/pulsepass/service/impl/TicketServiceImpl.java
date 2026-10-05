package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper mapper;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketMapper mapper) {

        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // 1. Buscar y validar User
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.userEmail()));

        // BR-TICKET-002: Usuario activo
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets");
        }

        // 2. Buscar y validar Event
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with code: " + request.eventCode()));

        // BR-TICKET-004: Evento publicado
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus()
            );
        }

        // BR-TICKET-005: Fecha futura
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase tickets for an event that has already occurred");
        }

        // BR-TICKET-006: Edad mínima evaluada en la fecha del evento
        if (event.getMinimumAge() > 0) {
            if (user.getProfile() == null || user.getProfile().getBirthDate() == null) {
                throw new BusinessRuleException("User profile birth date is required to evaluate minimum age");
            }
            int ageAtEvent = Period.between(user.getProfile().getBirthDate(), event.getEventDate().toLocalDate()).getYears();
            if (ageAtEvent < event.getMinimumAge()) {
                throw new BusinessRuleException(
                        "User does not meet the minimum age of " + event.getMinimumAge() + ". Age at event: " + ageAtEvent
                );
            }
        }

        // BR-TICKET-007: Validar capacidad
        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = (event.getVenue() != null) ? event.getVenue().getCapacity() : 0;
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Venue capacity reached (" + capacity + "). Event is sold out.");
        }

        // BR-TICKET-009: Calcular precio
        BigDecimal price = calculatePrice(request.type());

        // Generar código de ticket
        String ticketCode = "TCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        // BR-TICKET-008: Crear ticket con estado PAID
        Ticket ticket = new Ticket(
                ticketCode,
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event
        );
        Ticket savedTicket = ticketRepository.save(ticket);

        // Si la compra completa la capacidad, el evento cambia a SOLD_OUT en la misma transacción
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return mapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatusOrderByPurchaseDateAsc(eventCode, TicketStatus.PAID)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        // BR-TICKET-010 y BR-TICKET-011: Solo puede cancelarse un ticket PAID
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current ticket status: " + ticket.getStatus()
            );
        }

        // BR-TICKET-012: No puede cancelarse después de la fecha del evento
        if (LocalDateTime.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException("Cannot cancel ticket after the event date");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket updated = ticketRepository.save(ticket);
        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        // BR-TICKET-013 y BR-TICKET-014: Solo puede marcarse como usado un ticket PAID
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current ticket status: " + ticket.getStatus()
            );
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket updated = ticketRepository.save(ticket);
        return mapper.toResponse(updated);
    }

    /**
     * Estrategia de precios encapsulada (PRD sección 28).
     */
    private BigDecimal calculatePrice(TicketType type) {
        if (type == null) {
            throw new BusinessRuleException("Ticket type is required");
        }
        return switch (type) {
            case GENERAL -> new BigDecimal("50.00");
            case STUDENT -> new BigDecimal("35.00");
            case VIP -> new BigDecimal("100.00");
            case BACKSTAGE -> new BigDecimal("150.00");
        };
    }
}
