package com.example.qlyBDS.models;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "UserPropertyAccess")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPropertyAccess extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "propertyId", nullable = false)
    private Property property;

    private LocalDateTime accessGrantedAt;


}
