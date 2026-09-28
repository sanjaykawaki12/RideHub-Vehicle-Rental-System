package com.rental.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="wishlists", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","vehicle_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wishlist { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user; @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="vehicle_id",nullable=false) private Vehicle vehicle; }
