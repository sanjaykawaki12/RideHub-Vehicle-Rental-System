package com.rental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="support_tickets") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) @JsonIgnore private User user;
 @Column(nullable=false,length=40) private String category;
 @Column(nullable=false,length=160) private String subject;
 @Column(nullable=false,columnDefinition="TEXT") private String message;
 @Builder.Default @Column(nullable=false,length=30) private String status="OPEN";
 @Builder.Default @Column(nullable=false,length=20) private String priority="NORMAL";
 private LocalDateTime createdAt;
 private LocalDateTime updatedAt;
 @Column(columnDefinition="TEXT") private String adminReply;
 @PrePersist void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
