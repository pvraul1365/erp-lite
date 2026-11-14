package net.javaguides.erp_lite.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "products",
    schema = "public",
    indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(
        name = "sku",
        nullable = false,
        unique = true,
        length = 50
    )
    private String sku;

    @Column(
        name = "name",
        nullable = false,
        length = 200
    )
    private String name;

    @Column(
        name = "description",
        columnDefinition = "text",
        nullable = true
    )
    private String description;

    @Column(
        name = "price",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal price;

    @Column(
        name = "stock",
        nullable = false
    )
    @Builder.Default
    private Integer stock = 0;

    @Column(
        name = "category_id",
        length = 100,
        nullable = true
    )
    private String categoryId;

    @Column(
        name = "image_url",
        length = 500,
        nullable = true
    )
    private String imageUrl;

    @Column(
        name = "active",
        nullable = false
    )
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.REFRESH,
        fetch = FetchType.LAZY,
        orphanRemoval = false
    )
    @Builder.Default
    private Set<OrderProductEntity> orderProducts = new HashSet<>();

    @Version
    private Long version;
}
