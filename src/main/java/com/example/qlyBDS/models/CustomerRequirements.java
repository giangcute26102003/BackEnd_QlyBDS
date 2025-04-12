package com.example.qlyBDS.models;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customerrequirements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequirements extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requirementId;

    @ManyToOne
    @JoinColumn(name = "customerId", nullable = false)
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(precision = 15, scale = 2)
    private BigDecimal budgetMin;

    @Column(precision = 15, scale = 2)
    private BigDecimal budgetMax;

    @Column(columnDefinition = "TEXT")
    private String preferredLocation;

    private String propertyType;

    @Column(precision = 10, scale = 2)
    private BigDecimal sizeMin;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(columnDefinition = "TEXT")
    private String otherPreferences;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
