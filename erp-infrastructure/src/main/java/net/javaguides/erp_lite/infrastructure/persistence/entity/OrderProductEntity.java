package net.javaguides.erp_lite.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "order_products",
    schema = "public",
    indexes = {
        @Index(name = "idx_order_product_order", columnList = "order_id"),
        @Index(name = "idx_order_product_product", columnList = "product_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_product", columnNames = {"order_id", "product_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderProductEntity {

    @EmbeddedId
    private OrderProductId id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = CascadeType.MERGE,
        optional = false
    )
    @JoinColumn(
        name = "order_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_order_products_order",
            value = ConstraintMode.CONSTRAINT
        )
    )
    @MapsId("orderId")
    private OrderEntity order;

    @ManyToOne(
        fetch = FetchType.LAZY,
        cascade = CascadeType.MERGE,
        optional = false
    )
    @JoinColumn(
        name = "product_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_order_products_product",
            value = ConstraintMode.CONSTRAINT
        )
    )
    @MapsId("productId")
    private ProductEntity product;

    @Column(
        name = "product_name",
        nullable = false,
        length = 200
    )
    private String productName;

    @Column(
        name = "quantity",
        nullable = false
    )
    private Integer quantity;

    @Column(
        name = "unit_price",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
        name = "subtotal",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void calculateSubtotal() {
        if (this.quantity != null && this.unitPrice != null) {
            this.subtotal = this.unitPrice.multiply(new BigDecimal(this.quantity));
        }
    }
}
