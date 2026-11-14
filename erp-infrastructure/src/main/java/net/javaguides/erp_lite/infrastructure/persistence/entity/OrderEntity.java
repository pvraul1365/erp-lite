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
    name = "orders",
    schema = "public",
    indexes = {
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_date", columnList = "order_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(
        name = "order_number",
        nullable = false,
        unique = true,
        length = 50
    )
    private String orderNumber;

    @Column(
        name = "customer_id",
        nullable = false
    )
    private Long customerId;

    @Column(
        name = "customer_name",
        nullable = false,
        length = 200
    )
    private String customerName;

    @Column(
        name = "created_by",
        nullable = false,
        length = 100
    )
    private String createdBy;

    @Column(
        name = "order_date",
        nullable = false
    )
    private LocalDateTime orderDate;

    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    @Builder.Default
    private String status = "PENDING";

    @Column(
        name = "total_amount",
        nullable = false,
        precision = 15,
        scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
        name = "currency",
        nullable = false,
        length = 3
    )
    @Builder.Default
    private String currency = "USD";

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
        mappedBy = "order",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private Set<OrderProductEntity> orderProducts = new HashSet<>();

    @Version
    private Long version;

    public void addOrderProduct(OrderProductEntity orderProduct) {
        this.orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    public void removeOrderProduct(OrderProductEntity orderProduct) {
        this.orderProducts.remove(orderProduct);
        orderProduct.setOrder(null);
    }
}
