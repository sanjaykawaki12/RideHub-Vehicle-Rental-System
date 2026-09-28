package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.*;
import com.rental.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users; private final OwnerRepository owners; private final ShopRepository shops; private final VehicleRepository vehicles; private final BookingRepository bookings; private final PaymentRepository payments;
    public AdminController(UserRepository u,OwnerRepository o,ShopRepository s,VehicleRepository v,BookingRepository b,PaymentRepository p){users=u;owners=o;shops=s;vehicles=v;bookings=b;payments=p;}
    @GetMapping("/summary") public ResponseEntity<ApiResponse<Map<String,Object>>> summary(){Map<String,Object> m=new LinkedHashMap<>();m.put("customers",users.count());m.put("owners",owners.count());m.put("shops",shops.count());m.put("vehicles",vehicles.count());m.put("bookings",bookings.count());m.put("payments",payments.count());m.put("completedBookings",bookings.findAll().stream().filter(x->x.getStatus()==com.rental.entity.enums.BookingStatus.COMPLETED).count());return ResponseEntity.ok(ApiResponse.ok("Admin summary",m));}
    @GetMapping("/customers") public ResponseEntity<ApiResponse<List<User>>> customers(){return ResponseEntity.ok(ApiResponse.ok("Customers fetched",users.findAll()));}
    @GetMapping("/owners") public ResponseEntity<ApiResponse<List<Owner>>> ownerList(){return ResponseEntity.ok(ApiResponse.ok("Owners fetched",owners.findAll()));}
    @GetMapping("/shops") public ResponseEntity<ApiResponse<List<Shop>>> shopList(){return ResponseEntity.ok(ApiResponse.ok("Shops fetched",shops.findAll()));}
    @GetMapping("/vehicles") public ResponseEntity<ApiResponse<List<Vehicle>>> vehicleList(){return ResponseEntity.ok(ApiResponse.ok("Vehicles fetched",vehicles.findAll()));}
    @GetMapping("/bookings") public ResponseEntity<ApiResponse<List<Booking>>> bookingList(){return ResponseEntity.ok(ApiResponse.ok("Bookings fetched",bookings.findAll()));}
    @GetMapping("/reports") public ResponseEntity<ApiResponse<Map<String,Object>>> reports(){
        Map<String,Object> m=new LinkedHashMap<>(); List<Booking> all=bookings.findAll();
        long completed=all.stream().filter(b->b.getStatus()==com.rental.entity.enums.BookingStatus.COMPLETED).count();
        BigDecimal revenue=all.stream().filter(b->b.getStatus()==com.rental.entity.enums.BookingStatus.COMPLETED).map(Booking::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        m.put("totalBookings",all.size());m.put("completedBookings",completed);m.put("cancelledBookings",all.stream().filter(b->b.getStatus()==com.rental.entity.enums.BookingStatus.CANCELLED).count());m.put("pendingApproval",all.stream().filter(b->b.getStatus()==com.rental.entity.enums.BookingStatus.PENDING_OWNER_APPROVAL).count());m.put("revenue",revenue);m.put("successPayments",payments.countByStatus(com.rental.entity.enums.PaymentStatus.SUCCESS));
        return ResponseEntity.ok(ApiResponse.ok("Reports fetched",m));
    }
}
