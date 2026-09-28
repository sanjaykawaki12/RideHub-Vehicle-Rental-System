package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.entity.DrivingLicense;
import com.rental.security.AuthPrincipal;
import com.rental.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/user/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DrivingLicense>> upload(@AuthenticationPrincipal AuthPrincipal principal,
                                                                @RequestParam String licenseNumber,
                                                                @RequestParam("file") MultipartFile file) {
        DrivingLicense license = licenseService.upload(principal.id(), licenseNumber, file);
        return ResponseEntity.ok(ApiResponse.ok("License uploaded", license));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DrivingLicense>>> myLicenses(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Licenses fetched", licenseService.getByUser(principal.id())));
    }
}
