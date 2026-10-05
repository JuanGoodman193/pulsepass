package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper mapper;

    @InjectMocks
    private TicketServiceImpl service;

    @Test
    @DisplayName("TEST-TICKET-001: Compra válida -> ticket PAID")
    void shouldPurchaseTicketSuccessfully() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = new TicketResponse(
                1L, "TCK-12345", TicketType.GENERAL, new BigDecimal("50.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest"
        );
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        // ACT
        TicketResponse result = service.purchase(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @ParameterizedTest
    @CsvSource({
            "GENERAL, 50.00",
            "STUDENT, 35.00",
            "VIP, 100.00",
            "BACKSTAGE, 150.00"
    })
    @DisplayName("La compra aplica el precio definido para cada tipo de ticket")
    void shouldApplyConfiguredPriceForTicketType(TicketType type, String expectedPrice) {
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                type
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.purchase(request);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getPrice())
                .isEqualByComparingTo(expectedPrice);
    }

    @Test
    @DisplayName("Un tipo de ticket nulo se rechaza antes de guardar")
    void shouldRejectPurchaseWithoutTicketType() {
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                null
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026"))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ticket type is required");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-002: Usuario inexistente -> ResourceNotFoundException")
    void shouldThrowExceptionWhenUserDoesNotExist() {
        // ARRANGE
        PurchaseTicketRequest request = new PurchaseTicketRequest("unknown@email.com", "CMF-2026", TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase("unknown@email.com")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-003: Usuario inactivo -> BusinessRuleException")
    void shouldThrowExceptionWhenUserIsInactive() {
        // ARRANGE
        User user = createAdultUser("miguel@email.com", false);
        PurchaseTicketRequest request = new PurchaseTicketRequest("miguel@email.com", "CMF-2026", TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Inactive user");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-004: Evento DRAFT -> BusinessRuleException")
    void shouldThrowExceptionWhenEventIsDraft() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.DRAFT, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-005: Evento CANCELLED -> BusinessRuleException")
    void shouldThrowExceptionWhenEventIsCancelled() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.CANCELLED, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-006: Usuario menor de edad -> BusinessRuleException")
    void shouldThrowExceptionWhenUserDoesNotMeetMinimumAge() {
        // ARRANGE
        // Laura, 17 años al momento del evento
        User user = new User("laura17", "laura@email.com", true);
        UserProfile profile = new UserProfile(
                "Laura", "Perez", null, null,
                LocalDate.now().plusMonths(2).minusYears(17)
        );
        user.assignProfile(profile);

        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("laura@email.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("laura@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-007: Evento sin capacidad -> BusinessRuleException")
    void shouldThrowExceptionWhenVenueCapacityIsReached() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 3, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(3L);

        // ACT & ASSERT
        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Venue capacity reached");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-008: Último ticket disponible -> guardar ticket y cambiar evento a SOLD_OUT")
    void shouldUpdateEventToSoldOutWhenLastTicketIsPurchased() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 3, 18);
        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.VIP);

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = new TicketResponse(
                3L, "TCK-LAST", TicketType.VIP, new BigDecimal("100.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest"
        );
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        // ACT
        TicketResponse result = service.purchase(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("TEST-TICKET-009: Cancelar ticket PAID -> CANCELLED")
    void shouldCancelPaidTicket() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        Ticket ticket = new Ticket("TCK-001", TicketType.GENERAL, new BigDecimal("50.00"), TicketStatus.PAID, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = new TicketResponse(
                1L, "TCK-001", TicketType.GENERAL, new BigDecimal("50.00"),
                TicketStatus.CANCELLED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Event"
        );
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        // ACT
        TicketResponse result = service.cancel("TCK-001");

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("TEST-TICKET-010: Cancelar ticket USED -> BusinessRuleException")
    void shouldThrowExceptionWhenCancellingUsedTicket() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        Ticket ticket = new Ticket("TCK-001", TicketType.GENERAL, new BigDecimal("50.00"), TicketStatus.USED, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-001")).thenReturn(Optional.of(ticket));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.cancel("TCK-001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID tickets can be cancelled");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST-TICKET-011: Marcar PAID como usado -> USED")
    void shouldMarkPaidTicketAsUsed() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        Ticket ticket = new Ticket("TCK-001", TicketType.GENERAL, new BigDecimal("50.00"), TicketStatus.PAID, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = new TicketResponse(
                1L, "TCK-001", TicketType.GENERAL, new BigDecimal("50.00"),
                TicketStatus.USED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Event"
        );
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        // ACT
        TicketResponse result = service.markAsUsed("TCK-001");

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("TEST-TICKET-012: Usar ticket CANCELLED -> BusinessRuleException")
    void shouldThrowExceptionWhenMarkingCancelledTicketAsUsed() {
        // ARRANGE
        User user = createAdultUser("andrea@email.com", true);
        Event event = createEvent("CMF-2026", EventStatus.PUBLISHED, 10, 18);
        Ticket ticket = new Ticket("TCK-001", TicketType.GENERAL, new BigDecimal("50.00"), TicketStatus.CANCELLED, LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-001")).thenReturn(Optional.of(ticket));

        // ACT & ASSERT
        assertThatThrownBy(() -> service.markAsUsed("TCK-001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID tickets can be marked as used");

        verify(ticketRepository, never()).save(any());
    }

    private User createAdultUser(String email, boolean active) {
        User user = new User("user1", email, active);
        UserProfile profile = new UserProfile(
                "First", "Last", "123", "City",
                LocalDate.now().minusYears(25)
        );
        user.assignProfile(profile);
        return user;
    }

    private Event createEvent(String eventCode, EventStatus status, int capacity, int minAge) {
        Venue venue = new Venue("VEN-01", "Marina Center", "Santa Marta", "Address", capacity, true);
        Event event = new Event(
                eventCode, "Caribbean Music Fest", "Description",
                EventCategory.MUSIC, status, LocalDateTime.now().plusMonths(2), minAge, venue
        );
        return event;
    }
}
