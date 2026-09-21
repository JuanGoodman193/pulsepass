package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByVenue_CodeOrderByEventDateAsc(String venueCode);

    @Query("""
            select distinct e
            from Event e
            join e.artists a
            where a.stageName = :stageName
            order by e.eventDate asc
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    @Query("""
            select distinct e
            from Event e
            join e.artists a
            where e.venue.city = :city
              and a.stageName = :stageName
            order by e.eventDate asc
            """)
    List<Event> findByCityAndArtist(@Param("city") String city, @Param("stageName") String stageName);

    @Query("""
            select distinct e
            from Event e
            join e.artists a
            where e.status = com.pulsepass.domain.EventStatus.PUBLISHED
              and e.eventDate > :fromDate
              and e.venue.city = :city
              and lower(a.stageName) like lower(concat('%', :artistText, '%'))
            order by e.eventDate asc
            """)
    List<Event> findRecommended(@Param("fromDate") LocalDateTime fromDate,
                                @Param("city") String city,
                                @Param("artistText") String artistText);
}
