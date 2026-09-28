package com.rental.controller;
import com.rental.dto.ApiResponse; import com.rental.entity.Notification; import com.rental.repository.NotificationRepository; import com.rental.security.AuthPrincipal; import org.springframework.http.ResponseEntity; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/user/notifications") public class NotificationController{
 private final NotificationRepository repo; public NotificationController(NotificationRepository r){repo=r;}
 @GetMapping public ResponseEntity<ApiResponse<List<Notification>>> list(@AuthenticationPrincipal AuthPrincipal p){return ResponseEntity.ok(ApiResponse.ok("Notifications fetched",repo.findByUserIdOrderByCreatedAtDesc(p.id())));}
 @GetMapping("/unread-count") public ResponseEntity<ApiResponse<Long>> unread(@AuthenticationPrincipal AuthPrincipal p){return ResponseEntity.ok(ApiResponse.ok("Unread count",repo.countByUserIdAndReadFalse(p.id())));}
 @PatchMapping("/{id}/read") public ResponseEntity<ApiResponse<Notification>> read(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id){Notification n=repo.findById(id).orElseThrow();if(!n.getUser().getId().equals(p.id())) throw new IllegalArgumentException("Not allowed");n.setRead(true);return ResponseEntity.ok(ApiResponse.ok("Marked read",repo.save(n)));}
}
