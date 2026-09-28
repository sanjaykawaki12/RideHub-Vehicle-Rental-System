package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.User;
import com.rental.repository.UserRepository;
import com.rental.security.AuthPrincipal;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/user/profile")
public class ProfileController {
    private final UserRepository users; private final PasswordEncoder encoder;
    public ProfileController(UserRepository u,PasswordEncoder e){users=u;encoder=e;}
    @GetMapping public ResponseEntity<ApiResponse<User>> me(@AuthenticationPrincipal AuthPrincipal p){return ResponseEntity.ok(ApiResponse.ok("Profile fetched",users.findById(p.id()).orElseThrow()));}
    @PutMapping public ResponseEntity<ApiResponse<User>> update(@AuthenticationPrincipal AuthPrincipal p,@RequestBody ProfileRequest r){
        User u=users.findById(p.id()).orElseThrow(); if(r.name!=null&&!r.name.isBlank())u.setName(r.name.trim()); if(r.phone!=null)u.setPhone(r.phone.trim());
        return ResponseEntity.ok(ApiResponse.ok("Profile updated",users.save(u)));
    }
    @PatchMapping("/password") public ResponseEntity<ApiResponse<Void>> password(@AuthenticationPrincipal AuthPrincipal p,@RequestBody PasswordRequest r){
        User u=users.findById(p.id()).orElseThrow(); if(r.currentPassword==null||!encoder.matches(r.currentPassword,u.getPassword())) throw new IllegalArgumentException("Current password is incorrect"); if(r.newPassword==null||r.newPassword.length()<6) throw new IllegalArgumentException("New password must be at least 6 characters"); u.setPassword(encoder.encode(r.newPassword)); users.save(u); return ResponseEntity.ok(ApiResponse.ok("Password updated",null));
    }
    @Data public static class ProfileRequest{public String name;public String phone;}
    @Data public static class PasswordRequest{public String currentPassword;public String newPassword;}
}
