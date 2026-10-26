package com.khaata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A shop-owner login. Passwords are stored only as BCrypt hashes.
 * This project has a single user type (the owner), modelled with a simple role
 * so the design can grow to multiple roles later without a schema rewrite.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login key — the normalized shop name. Unique across all shops. */
    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", length = 120)
    private String displayName;

    /** The shop's name (brand shown in reminder emails). Required. */
    @Column(name = "shop_name", nullable = false, length = 120)
    private String shopName;

    /** The person who owns/runs the shop. Required. */
    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    /** Owner contact + payment details, set on the profile page. */
    @Column(length = 20)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(name = "upi_id", length = 80)
    private String upiId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
