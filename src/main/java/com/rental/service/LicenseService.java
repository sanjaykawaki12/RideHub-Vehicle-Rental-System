package com.rental.service;

import com.rental.entity.DrivingLicense;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface LicenseService {
    DrivingLicense upload(Long userId, String licenseNumber, MultipartFile file);
    List<DrivingLicense> getByUser(Long userId);
}
