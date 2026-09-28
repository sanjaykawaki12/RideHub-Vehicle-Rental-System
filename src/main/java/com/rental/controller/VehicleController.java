package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.VehicleRequest;
import com.rental.entity.Vehicle;
import com.rental.entity.enums.VehicleStatus;
import com.rental.security.AuthPrincipal;
import com.rental.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // Public: user page — list of vehicles for a given shop
    @GetMapping("/vehicles/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<Vehicle>>> byShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.ok("Vehicles fetched", vehicleService.getVehiclesByShop(shopId)));
    }

    // Owner: add / update / delete / change status — scoped to their own shop only
    @PostMapping("/owner/shops/{shopId}/vehicles")
    public ResponseEntity<ApiResponse<Vehicle>> add(@AuthenticationPrincipal AuthPrincipal principal,
                                                      @PathVariable Long shopId,
                                                      @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleService.addVehicle(principal.id(), shopId, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle added", vehicle));
    }

    @PutMapping("/owner/vehicles/{vehicleId}")
    public ResponseEntity<ApiResponse<Vehicle>> update(@AuthenticationPrincipal AuthPrincipal principal,
                                                         @PathVariable Long vehicleId,
                                                         @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleService.updateVehicle(principal.id(), vehicleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle updated", vehicle));
    }

    @DeleteMapping("/owner/vehicles/{vehicleId}")
    public ResponseEntity<ApiResponse<Object>> delete(@AuthenticationPrincipal AuthPrincipal principal,
                                                        @PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(principal.id(), vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle deleted", null));
    }

    @PatchMapping("/owner/vehicles/{vehicleId}/status")
    public ResponseEntity<ApiResponse<Vehicle>> status(@AuthenticationPrincipal AuthPrincipal principal,
                                                         @PathVariable Long vehicleId,
                                                         @RequestParam VehicleStatus status) {
        Vehicle vehicle = vehicleService.updateStatus(principal.id(), vehicleId, status);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status updated", vehicle));
    }

    // Owner: upload/replace a vehicle's photo
    @PostMapping(value = "/owner/vehicles/{vehicleId}/image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Vehicle>> uploadImage(@AuthenticationPrincipal AuthPrincipal principal,
                                                              @PathVariable Long vehicleId,
                                                              @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Vehicle vehicle = vehicleService.uploadImage(principal.id(), vehicleId, file);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle image uploaded", vehicle));
    }
}
