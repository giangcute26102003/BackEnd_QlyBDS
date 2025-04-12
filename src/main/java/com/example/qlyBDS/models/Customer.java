package com.example.qlyBDS.models;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer customerId;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    private LocalDate dob;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}