package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pulsepass_test")
            .withUsername("pulsepass")
            .withPassword("pulsepass");

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void flywayAppliesAllMigrations() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(appliedMigrations).isEqualTo(3);
    }

    @Test
    void persistsVenueEventAndArtistsAndFindsPublishedEvents() {
        Venue venue = venueRepository.saveAndFlush(new Venue(
                unique("VEN"), "Marina Convention Center", "Santa Marta", "Carrera 1", 5000, true));
        Artist artist = artistRepository.findByStageName("Solar Beat").orElseThrow();
        Event event = new Event(unique("EVT"), "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18, venue);
        event.addArtist(artist);
        eventRepository.saveAndFlush(event);

        List<Event> published = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);
        List<Event> byArtist = eventRepository.findByArtistStageName("Solar Beat");

        assertThat(published).extracting(Event::getEventCode).contains(event.getEventCode());
        assertThat(byArtist).extracting(Event::getEventCode).contains(event.getEventCode());
        assertThat(eventRepository.findByVenue_CodeOrderByEventDateAsc(venue.getCode()))
                .extracting(Event::getEventCode).containsExactly(event.getEventCode());
    }

    @Test
    void enforcesUniqueVenueCode() {
        String code = unique("VEN");
        venueRepository.saveAndFlush(new Venue(code, "Venue one", "Santa Marta", "Address 1", 100, true));

        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue(code, "Venue two", "Santa Marta", "Address 2", 200, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsUserProfileAndCountsPaidTickets() {
        Venue venue = venueRepository.saveAndFlush(new Venue(
                unique("VEN"), "Arena", "Bogota", "Address", 1000, true));
        Event event = eventRepository.saveAndFlush(new Event(unique("EVT"), "Tech Summit", "Conference",
                EventCategory.TECHNOLOGY, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), 0, venue));
        User user = userRepository.saveAndFlush(new User(unique("user"), unique("mail") + "@example.com", true));
        UserProfile profile = new UserProfile("Andrea", "Lopez", "3000000000", "Bogota", LocalDate.of(1995, 5, 10));
        user.assignProfile(profile);
        userProfileRepository.saveAndFlush(profile);

        ticketRepository.saveAndFlush(new Ticket(unique("TCK"), TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), user, event));
        ticketRepository.saveAndFlush(new Ticket(unique("TCK"), TicketType.GENERAL, new BigDecimal("120000.00"),
                TicketStatus.RESERVED, LocalDateTime.now(), user, event));

        assertThat(userRepository.findByEmailIgnoreCase(user.getEmail())).isPresent();
        assertThat(ticketRepository.findByUser_EmailAndStatusOrderByPurchaseDateAsc(user.getEmail(), TicketStatus.PAID))
                .hasSize(1);
        assertThat(ticketRepository.countPaidByEventCode(event.getEventCode())).isOne();
    }

    private static String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
}
