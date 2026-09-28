package com.rental.repository;

import com.rental.entity.Vehicle;
import com.rental.entity.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByShopId(Long shopId);
    List<Vehicle> findByShopIdAndStatus(Long shopId, VehicleStatus status);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    long countByShopIdAndStatus(Long shopId, VehicleStatus status);
    long countByShopId(Long shopId);
}
