package com.rental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "driving_licenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DrivingLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 30)
    private String licenseNumber;

    @Column(nullable = false)
    private String fileUrl;

    @Builder.Default
    private boolean verified = false;

    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() { this.uploadedAt = LocalDateTime.now(); }
}
