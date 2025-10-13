package com.example.datn_realeaste_crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "property")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Property {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Integer propertyId;
    
    @Column(name = "address_property", columnDefinition = "TEXT")
    private String addressProperty;
    
    @Column(name = "property_type", nullable = false)
    private String propertyType;
    
    @Column(name = "size", precision = 10, scale = 2)
    private BigDecimal size;
    
    @Column(name = "floor")
    private Integer floor;
    
    @Column(name = "thumbnail")
    private String thumbnail;
    
    @Column(name = "bedrooms")
    private Integer bedrooms;
    
    @Column(name = "bathrooms")
    private Integer bathrooms;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;
    
    @Column(name = "legal_documents", columnDefinition = "TEXT")
    private String legalDocuments;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "availability", nullable = false)
    private AvailabilityStatus availability;
    
    @Column(name = "phone_owner")
    private String phoneOwner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PropertyOwnership> ownerships = new HashSet<>();
    
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods to manage bidirectional relationships
    public void addImage(PropertyImage image) {
        images.add(image);
        image.setProperty(this);
    }
    
    public void removeImage(PropertyImage image) {
        images.remove(image);
        image.setProperty(null);
    }
    
    public void addOwnership(PropertyOwnership ownership) {
        ownerships.add(ownership);
        ownership.setProperty(this);
    }
    
    public void removeOwnership(PropertyOwnership ownership) {
        ownerships.remove(ownership);
        ownership.setProperty(null);
    }
    
    public void addReview(Review review) {
        reviews.add(review);
        review.setProperty(this);
    }
    
    public void removeReview(Review review) {
        reviews.remove(review);
        review.setProperty(null);
    }
}