package com.example.qlyBDS.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "district")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String name;
}