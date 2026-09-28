package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.SupportMessage;
import com.rental.entity.SupportTicket;
import com.rental.entity.User;
import com.rental.repository.SupportMessageRepository;
import com.rental.repository.SupportTicketRepository;
import com.rental.repository.UserRepository;
import com.rental.security.AuthPrincipal;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SupportController {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final UserRepository users;

    public SupportController(SupportTicketRepository t, SupportMessageRepository m, UserRepository u) {
        tickets = t; messages = m; users = u;
    }

    @GetMapping("/user/support/tickets")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> list(@AuthenticationPrincipal AuthPrincipal p) {
        return ResponseEntity.ok(ApiResponse.ok("Tickets fetched", ticketViews(tickets.findByUserIdOrderByCreatedAtDesc(p.id()))));
    }

    @PostMapping("/user/support/tickets")
    public ResponseEntity<ApiResponse<SupportTicket>> create(@AuthenticationPrincipal AuthPrincipal p, @RequestBody TicketRequest r) {
        User u = users.findById(p.id()).orElseThrow();
        SupportTicket t = SupportTicket.builder().user(u).category(r.category).subject(r.subject)
                .message(r.message).priority(r.priority == null ? "NORMAL" : r.priority).build();
        t = tickets.save(t);
        messages.save(SupportMessage.builder().ticket(t).senderType("CUSTOMER").senderName(u.getName()).message(r.message).build());
        return ResponseEntity.ok(ApiResponse.ok("Support ticket created", t));
    }

    @PostMapping("/user/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessage>> customerMessage(@AuthenticationPrincipal AuthPrincipal p,
                                                                         @PathVariable Long id,
                                                                         @RequestBody MessageRequest r) {
        SupportTicket t = tickets.findById(id).orElseThrow();
        if (!t.getUser().getId().equals(p.id())) throw new IllegalArgumentException("You can only reply to your own ticket");
        User u = users.findById(p.id()).orElseThrow();
        t.setStatus("OPEN");
        tickets.save(t);
        return ResponseEntity.ok(ApiResponse.ok("Message sent", messages.save(
                SupportMessage.builder().ticket(t).senderType("CUSTOMER").senderName(u.getName()).message(r.message).build()
        )));
    }

    @GetMapping("/user/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessage>>> customerMessages(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id) {
        SupportTicket t = tickets.findById(id).orElseThrow();
        if (!t.getUser().getId().equals(p.id())) throw new IllegalArgumentException("Access denied");
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched", messages.findByTicketIdOrderByCreatedAtAsc(id)));
    }

    @GetMapping("/admin/support/tickets")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> all() {
        return ResponseEntity.ok(ApiResponse.ok("Tickets fetched", ticketViews(tickets.findAllByOrderByCreatedAtDesc())));
    }

    @GetMapping("/admin/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessage>>> adminMessages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched", messages.findByTicketIdOrderByCreatedAtAsc(id)));
    }

    @PostMapping("/admin/support/tickets/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessage>> adminMessage(@PathVariable Long id, @RequestBody MessageRequest r) {
        SupportTicket t = tickets.findById(id).orElseThrow();
        t.setStatus("IN_PROGRESS");
        tickets.save(t);
        return ResponseEntity.ok(ApiResponse.ok("Reply sent", messages.save(
                SupportMessage.builder().ticket(t).senderType("SUPPORT").senderName("RideHub Support").message(r.message).build()
        )));
    }

    @PatchMapping("/admin/support/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicket>> update(@PathVariable Long id, @RequestBody AdminUpdate r) {
        SupportTicket t = tickets.findById(id).orElseThrow();
        if (r.status != null) t.setStatus(r.status);
        if (r.adminReply != null && !r.adminReply.isBlank()) {
            t.setAdminReply(r.adminReply);
            messages.save(SupportMessage.builder().ticket(t).senderType("SUPPORT").senderName("RideHub Support").message(r.adminReply).build());
        }
        return ResponseEntity.ok(ApiResponse.ok("Ticket updated", tickets.save(t)));
    }

    private List<Map<String,Object>> ticketViews(List<SupportTicket> list) {
        return list.stream().map(t -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", t.getId()); m.put("category", t.getCategory()); m.put("subject", t.getSubject());
            m.put("message", t.getMessage()); m.put("status", t.getStatus()); m.put("priority", t.getPriority());
            m.put("createdAt", t.getCreatedAt()); m.put("updatedAt", t.getUpdatedAt()); m.put("adminReply", t.getAdminReply());
            m.put("customerName", t.getUser().getName()); m.put("customerEmail", t.getUser().getEmail()); m.put("customerId", t.getUser().getId());
            return m;
        }).collect(Collectors.toList());
    }

    @Data public static class TicketRequest { @NotBlank public String category; @NotBlank public String subject; @NotBlank public String message; public String priority; }
    @Data public static class MessageRequest { @NotBlank public String message; }
    @Data public static class AdminUpdate { public String status; public String adminReply; }
}
