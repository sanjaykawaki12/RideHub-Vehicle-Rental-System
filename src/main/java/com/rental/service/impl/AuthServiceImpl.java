package com.rental.service.impl;

import com.rental.dto.AuthResponse;
import com.rental.dto.LoginRequest;
import com.rental.dto.RegisterRequest;
import com.rental.entity.Owner;
import com.rental.entity.User;
import com.rental.entity.enums.Role;
import com.rental.exception.ApiException;
import com.rental.repository.OwnerRepository;
import com.rental.repository.UserRepository;
import com.rental.security.JwtUtil;
import com.rental.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, OwnerRepository ownerRepository,
                            PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String hashed = passwordEncoder.encode(request.getPassword());

        if (request.getRole() == Role.OWNER) {
            if (ownerRepository.existsByEmail(request.getEmail())) {
                throw new ApiException("An owner account with this email already exists", HttpStatus.CONFLICT);
            }
            Owner owner = ownerRepository.save(Owner.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(hashed)
                     .phone(request.getPhone())
                    .build());

            String token = jwtUtil.generateToken(owner.getId(), owner.getEmail(), Role.OWNER.name());
            return AuthResponse.builder()
                    .token(token).id(owner.getId()).name(owner.getName())
                    .email(owner.getEmail()).role(Role.OWNER.name()).build();
        } else {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ApiException("A user account with this email already exists", HttpStatus.CONFLICT);
            }
            User user = userRepository.save(User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(hashed)
                    .phone(request.getPhone())
                    .build());

            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
            return AuthResponse.builder()
                    .token(token).id(user.getId()).name(user.getName())
                    .email(user.getEmail()).role(user.getRole().name()).build();
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        if (request.getRole() == Role.OWNER) {
            Owner owner = ownerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

            if (!passwordEncoder.matches(request.getPassword(), owner.getPassword())) {
                throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            String token = jwtUtil.generateToken(owner.getId(), owner.getEmail(), Role.OWNER.name());
            return AuthResponse.builder()
                    .token(token).id(owner.getId()).name(owner.getName())
                    .email(owner.getEmail()).role(Role.OWNER.name()).build();
        } else {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), Role.USER.name());
            return AuthResponse.builder()
                    .token(token).id(user.getId()).name(user.getName())
                    .email(user.getEmail()).role(Role.USER.name()).build();
        }
    }
}
