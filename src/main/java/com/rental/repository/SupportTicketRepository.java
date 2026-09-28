package com.rental.repository;
import com.rental.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long>{List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
List<SupportTicket> findAllByOrderByCreatedAtDesc();}
