package com.example.qlyBDS.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "propertyId", nullable = false)
    private Property property;

    @Column(name = "image_url")
    private String imageUrl;
}
