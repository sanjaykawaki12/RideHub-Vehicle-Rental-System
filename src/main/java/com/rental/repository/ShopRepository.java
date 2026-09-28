package com.rental.repository;

import com.rental.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByOwnerId(Long ownerId);

    // Haversine formula: returns shops within radiusKm ordered by nearest first
    @Query(value = "SELECT s.*, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(s.latitude)))) AS distance_km " +
            "FROM shops s " +
            "HAVING distance_km <= :radiusKm " +
            "ORDER BY distance_km ASC", nativeQuery = true)
    List<Shop> findNearbyShops(@Param("lat") Double lat,
                                @Param("lng") Double lng,
                                @Param("radiusKm") Double radiusKm);
}
