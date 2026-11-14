package net.javaguides.erp_lite.infrastructure.persistence.repository;

import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByCustomerId(Long customerId);

    Page<OrderEntity> findByCustomerId(Long customerId, Pageable pageable);

    List<OrderEntity> findByStatus(String status);

    Page<OrderEntity> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
    Optional<OrderEntity> findByIdWithDetails(UUID id);

    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.customerId = :customerId
        AND o.status = :status
        """)
    Page<OrderEntity> findByCustomerIdAndStatus(
        @Param("customerId") Long customerId,
        @Param("status") String status,
        Pageable pageable
    );

    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.orderDate BETWEEN :startDate AND :endDate
        ORDER BY o.orderDate DESC
        """)
    Page<OrderEntity> findOrdersByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.totalAmount >= :minAmount
        AND o.totalAmount <= :maxAmount
        """)
    List<OrderEntity> findByAmountRange(
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount
    );

    @Query("""
        SELECT COUNT(o) FROM OrderEntity o
        WHERE o.status = :status
        """)
    long countByStatus(@Param("status") String status);

    @Query("""
        SELECT SUM(o.totalAmount) FROM OrderEntity o
        WHERE o.status = 'COMPLETED'
        AND o.orderDate >= :fromDate
        """)
    BigDecimal calculateTotalRevenue(@Param("fromDate") LocalDateTime fromDate);

    @Query("""
        SELECT o FROM OrderEntity o
        WHERE o.createdBy = :createdBy
        ORDER BY o.orderDate DESC
        """)
    Page<OrderEntity> findOrdersByCreator(@Param("createdBy") String createdBy, Pageable pageable);
}
