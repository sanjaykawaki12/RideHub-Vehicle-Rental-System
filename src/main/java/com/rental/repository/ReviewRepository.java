package com.rental.repository; import com.rental.entity.Review; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ReviewRepository extends JpaRepository<Review,Long>{ List<Review> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId); Optional<Review> findByUserIdAndVehicleId(Long userId,Long vehicleId); }
