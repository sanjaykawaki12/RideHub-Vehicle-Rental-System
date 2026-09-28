package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.*;
import com.rental.entity.enums.BookingStatus;
import com.rental.repository.*;
import com.rental.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class ReviewController {
 private final ReviewRepository repo; private final UserRepository users; private final VehicleRepository vehicles; private final BookingRepository bookings;
 public ReviewController(ReviewRepository r,UserRepository u,VehicleRepository v,BookingRepository b){repo=r;users=u;vehicles=v;bookings=b;}
 @GetMapping("/vehicles/{vehicleId}/reviews") public ResponseEntity<ApiResponse<List<Review>>> list(@PathVariable Long vehicleId){return ResponseEntity.ok(ApiResponse.ok("Reviews fetched",repo.findByVehicleIdOrderByCreatedAtDesc(vehicleId)));}
 @PostMapping("/user/reviews") public ResponseEntity<ApiResponse<Review>> add(@AuthenticationPrincipal AuthPrincipal p,@RequestBody ReviewRequest r){
   if(r.vehicleId==null) throw new IllegalArgumentException("Vehicle is required");
   if(r.rating==null||r.rating<1||r.rating>5) throw new IllegalArgumentException("Rating must be 1-5");
   boolean completed=bookings.findByUserId(p.id()).stream().anyMatch(b->b.getVehicle().getId().equals(r.vehicleId)&&b.getStatus()==BookingStatus.COMPLETED);
   if(!completed) throw new IllegalArgumentException("You can review a vehicle only after completing a booking");
   Review x=repo.findByUserIdAndVehicleId(p.id(),r.vehicleId).orElse(Review.builder().user(users.findById(p.id()).orElseThrow()).vehicle(vehicles.findById(r.vehicleId).orElseThrow()).build());
   x.setRating(r.rating);x.setComment(r.comment);return ResponseEntity.ok(ApiResponse.ok("Review saved",repo.save(x)));
 }
 public static class ReviewRequest{public Long vehicleId;public Integer rating;public String comment;}
}
