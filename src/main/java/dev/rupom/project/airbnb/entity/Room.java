package dev.rupom.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id",nullable = false)
    private Hotel hotel;
    @Column(nullable = false)
    private String type;
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal basePrice;
    @Column(columnDefinition = "TEXT[]")
    private String[] amenities;
    @Column(columnDefinition = "TEXT[]")
    private String[] photos;
    @Column(nullable = false)
    private Integer totalCount;
    @Column(nullable = false)
    private Integer capacity;
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Inventory> inventories = new ArrayList<>();
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
