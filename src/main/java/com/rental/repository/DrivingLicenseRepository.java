package com.rental.repository;

import com.rental.entity.DrivingLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DrivingLicenseRepository extends JpaRepository<DrivingLicense, Long> {
    List<DrivingLicense> findByUserId(Long userId);
    List<DrivingLicense> findAllByOrderByUploadedAtDesc();
}
