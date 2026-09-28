package com.rental.repository; import com.rental.entity.Wishlist; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WishlistRepository extends JpaRepository<Wishlist,Long>{ List<Wishlist> findByUserId(Long userId); Optional<Wishlist> findByUserIdAndVehicleId(Long userId,Long vehicleId); }
