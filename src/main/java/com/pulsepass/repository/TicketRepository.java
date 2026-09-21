package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUser_EmailOrderByPurchaseDateAsc(String email);

    List<Ticket> findByUser_EmailAndStatusOrderByPurchaseDateAsc(String email, TicketStatus status);

    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    @Query("""
            select count(t)
            from Ticket t
            where t.event.eventCode = :eventCode
              and t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidByEventCode(@Param("eventCode") String eventCode);

    @Query("""
            select t
            from Ticket t
            where t.event.eventDate > :fromDate
            order by t.event.eventDate asc
            """)
    List<Ticket> findForFutureEvents(@Param("fromDate") LocalDateTime fromDate);
}
