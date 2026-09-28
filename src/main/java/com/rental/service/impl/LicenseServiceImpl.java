package com.rental.service.impl;

import com.rental.entity.DrivingLicense;
import com.rental.entity.User;
import com.rental.exception.ApiException;
import com.rental.repository.DrivingLicenseRepository;
import com.rental.repository.UserRepository;
import com.rental.service.FileStorageService;
import com.rental.service.LicenseService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class LicenseServiceImpl implements LicenseService {

    private final DrivingLicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public LicenseServiceImpl(DrivingLicenseRepository licenseRepository, UserRepository userRepository,
                               FileStorageService fileStorageService) {
        this.licenseRepository = licenseRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public DrivingLicense upload(Long userId, String licenseNumber, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (file == null || file.isEmpty()) throw new ApiException("Please select a license file", HttpStatus.BAD_REQUEST);
        String url = fileStorageService.store(file, "licenses");

        DrivingLicense license = DrivingLicense.builder()
                .user(user)
                .licenseNumber(licenseNumber)
                .fileUrl(url)
                .verified(false)
                .build();
        return licenseRepository.save(license);
    }

    @Override
    public List<DrivingLicense> getByUser(Long userId) {
        return licenseRepository.findByUserId(userId);
    }
}
