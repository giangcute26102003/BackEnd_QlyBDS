package com.example.qlyBDS.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "property")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer propertyId;

    @Column(name = "addressProperty")
    private String addressProperty;

    @Column(nullable = false)
    private String propertyType;

    @Column(precision = 10, scale = 2)
    private BigDecimal size;

    private Integer floor;

    private String thumbnail;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String legalDocuments;

    private String availability;

    private String phoneOwner;

    @ManyToOne
    @JoinColumn(name = "districtId", nullable = false)
    private District district;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL)
    private List<PropertyImages> images;


}