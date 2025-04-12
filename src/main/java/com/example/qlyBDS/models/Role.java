package com.example.qlyBDS.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roleId;

    @Column(unique = true, nullable = false)
    private String roleName;

    private String description;

    public static String GIAMDOC = "GIAMDOC";
    public static String CHUYENVIEN = "CHUYENVIEN";
    public static String DAUCHU = "DAUCHU";


}