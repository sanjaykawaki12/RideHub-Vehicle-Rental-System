package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.DrivingLicense;
import com.rental.entity.Notification;
import com.rental.repository.DrivingLicenseRepository;
import com.rental.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/licenses")
public class LicenseAdminController {
    private final DrivingLicenseRepository licenses;
    private final NotificationRepository notifications;
    public LicenseAdminController(DrivingLicenseRepository l, NotificationRepository n){licenses=l;notifications=n;}
    @GetMapping public ResponseEntity<ApiResponse<List<DrivingLicense>>> all(){return ResponseEntity.ok(ApiResponse.ok("Licenses fetched",licenses.findAllByOrderByUploadedAtDesc()));}
    @PatchMapping("/{id}/verify") public ResponseEntity<ApiResponse<DrivingLicense>> verify(@PathVariable Long id,@RequestParam boolean verified){
        DrivingLicense l=licenses.findById(id).orElseThrow(); l.setVerified(verified); l=licenses.save(l);
        notifications.save(Notification.builder().user(l.getUser()).title(verified?"Driving license verified":"Driving license rejected").message(verified?"Your driving license has been verified. You can now book vehicles.":"Your driving license was rejected. Please upload a valid document.").build());
        return ResponseEntity.ok(ApiResponse.ok(verified?"License verified":"License rejected",l));
    }
}
