package net.javaguides.erp_lite.infrastructure.persistence.repository;

import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderProductEntity;
import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProductEntity, OrderProductId> {

    List<OrderProductEntity> findByOrderId(UUID orderId);

    List<OrderProductEntity> findByProductId(UUID productId);

    @Query("""
        SELECT op FROM OrderProductEntity op
        WHERE op.order.id = :orderId
        ORDER BY op.createdAt DESC
        """)
    List<OrderProductEntity> findLineItemsByOrder(@Param("orderId") UUID orderId);

    @Query("""
        SELECT COUNT(op) FROM OrderProductEntity op
        WHERE op.product.id = :productId
        """)
    long countOrdersContainingProduct(@Param("productId") UUID productId);

    @Query("""
        SELECT SUM(op.quantity) FROM OrderProductEntity op
        WHERE op.product.id = :productId
        """)
    Integer getTotalQuantitySoldForProduct(@Param("productId") UUID productId);
}
