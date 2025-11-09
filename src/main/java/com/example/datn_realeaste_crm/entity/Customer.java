package com.example.datn_realeaste_crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    // Encrypted fields with deterministic hash for lookups
    @Convert(converter = com.example.datn_realeaste_crm.security.crypto.StringAttributeEncryptor.class)
    @Column(name = "phone_number_enc")
    private String phoneNumber;
    
    @Column(name = "phone_hash", columnDefinition = "BINARY(32)")
    private byte[] phoneHash;
    
    @Convert(converter = com.example.datn_realeaste_crm.security.crypto.StringAttributeEncryptor.class)
    @Column(name = "email_enc")
    private String email;
    
    @Column(name = "email_hash", columnDefinition = "BINARY(32)")
    private byte[] emailHash;
    
    @Convert(converter = com.example.datn_realeaste_crm.security.crypto.StringAttributeEncryptor.class)
    @Column(name = "address_enc", columnDefinition = "BLOB")
    private String address;
    
    @Column(name = "dob")
    private LocalDate dob;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}