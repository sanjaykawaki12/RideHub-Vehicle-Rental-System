package com.rental.entity;
import com.fasterxml.jackson.annotation.JsonIgnore; import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) @JsonIgnore private User user; @Column(nullable=false,length=160) private String title; @Column(nullable=false,length=1000) private String message; @Builder.Default private boolean read=false; private LocalDateTime createdAt; @PrePersist void onCreate(){createdAt=LocalDateTime.now();} }
