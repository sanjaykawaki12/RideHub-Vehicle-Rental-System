package com.rental.service.impl;

import com.rental.dto.VehicleRequest;
import com.rental.entity.Shop;
import com.rental.entity.Vehicle;
import com.rental.entity.enums.VehicleStatus;
import com.rental.exception.ApiException;
import com.rental.repository.VehicleRepository;
import com.rental.service.FileStorageService;
import com.rental.service.ShopService;
import com.rental.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ShopService shopService;
    private final FileStorageService fileStorageService;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, ShopService shopService,
                               FileStorageService fileStorageService) {
        this.vehicleRepository = vehicleRepository;
        this.shopService = shopService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Vehicle addVehicle(Long ownerId, Long shopId, VehicleRequest request) {
        Shop shop = shopService.getShopOrThrow(shopId);
        shopService.assertOwnership(ownerId, shop);

        vehicleRepository.findByRegistrationNumber(request.getRegistrationNumber())
                .ifPresent(v -> { throw new ApiException("A vehicle with this registration number already exists", HttpStatus.CONFLICT); });

        Vehicle vehicle = Vehicle.builder()
                .shop(shop)
                .name(request.getName())
                .type(request.getType())
                .model(request.getModel())
                .registrationNumber(request.getRegistrationNumber())
                .pricePerDay(request.getPricePerDay())
                .imageUrl(request.getImageUrl())
                .status(VehicleStatus.AVAILABLE)
                .build();
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(Long ownerId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = getOwnedVehicle(ownerId, vehicleId);
        vehicle.setName(request.getName());
        vehicle.setType(request.getType());
        vehicle.setModel(request.getModel());
        vehicle.setPricePerDay(request.getPricePerDay());
        vehicle.setImageUrl(request.getImageUrl());
        return vehicleRepository.save(vehicle);
    }

    @Override
    public void deleteVehicle(Long ownerId, Long vehicleId) {
        Vehicle vehicle = getOwnedVehicle(ownerId, vehicleId);
        vehicleRepository.delete(vehicle);
    }

    @Override
    public Vehicle updateStatus(Long ownerId, Long vehicleId, VehicleStatus status) {
        Vehicle vehicle = getOwnedVehicle(ownerId, vehicleId);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle uploadImage(Long ownerId, Long vehicleId, MultipartFile file) {
        Vehicle vehicle = getOwnedVehicle(ownerId, vehicleId);
        String url = fileStorageService.store(file, "vehicles");
        vehicle.setImageUrl(url);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getVehiclesByShop(Long shopId) {
        return vehicleRepository.findByShopId(shopId);
    }

    private Vehicle getOwnedVehicle(Long ownerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ApiException("Vehicle not found", HttpStatus.NOT_FOUND));
        shopService.assertOwnership(ownerId, vehicle.getShop());
        return vehicle;
    }
}
