package com.rental.service;

import com.rental.dto.VehicleRequest;
import com.rental.entity.Vehicle;
import com.rental.entity.enums.VehicleStatus;
import java.util.List;

public interface VehicleService {
    Vehicle addVehicle(Long ownerId, Long shopId, VehicleRequest request);
    Vehicle updateVehicle(Long ownerId, Long vehicleId, VehicleRequest request);
    void deleteVehicle(Long ownerId, Long vehicleId);
    Vehicle updateStatus(Long ownerId, Long vehicleId, VehicleStatus status);
    Vehicle uploadImage(Long ownerId, Long vehicleId, org.springframework.web.multipart.MultipartFile file);
    List<Vehicle> getVehiclesByShop(Long shopId);
}
