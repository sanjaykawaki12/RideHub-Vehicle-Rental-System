package com.rental.controller;
import com.rental.dto.ApiResponse; import com.rental.entity.*; import com.rental.repository.*; import com.rental.security.AuthPrincipal; import org.springframework.http.ResponseEntity; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/user/wishlist") public class WishlistController{
 private final WishlistRepository repo; private final UserRepository users; private final VehicleRepository vehicles;
 public WishlistController(WishlistRepository r,UserRepository u,VehicleRepository v){repo=r;users=u;vehicles=v;}
 @GetMapping public ResponseEntity<ApiResponse<List<Wishlist>>> list(@AuthenticationPrincipal AuthPrincipal p){return ResponseEntity.ok(ApiResponse.ok("Wishlist fetched",repo.findByUserId(p.id())));}
 @PostMapping("/{vehicleId}") public ResponseEntity<ApiResponse<Wishlist>> add(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long vehicleId){ if(repo.findByUserIdAndVehicleId(p.id(),vehicleId).isPresent()) return ResponseEntity.ok(ApiResponse.ok("Already in wishlist",repo.findByUserIdAndVehicleId(p.id(),vehicleId).get())); Wishlist w=Wishlist.builder().user(users.findById(p.id()).orElseThrow()).vehicle(vehicles.findById(vehicleId).orElseThrow()).build(); return ResponseEntity.ok(ApiResponse.ok("Added to wishlist",repo.save(w))); }
 @DeleteMapping("/{vehicleId}") public ResponseEntity<ApiResponse<Void>> remove(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long vehicleId){repo.findByUserIdAndVehicleId(p.id(),vehicleId).ifPresent(repo::delete);return ResponseEntity.ok(ApiResponse.ok("Removed from wishlist",null));}
}
