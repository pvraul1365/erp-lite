package net.javaguides.erp_lite.infrastructure.persistence.repository;

import net.javaguides.erp_lite.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySku(String sku);

    List<ProductEntity> findByActiveTrue();

    Page<ProductEntity> findByActiveTrue(Pageable pageable);

    List<ProductEntity> findByCategoryId(String categoryId);

    Page<ProductEntity> findByCategoryId(String categoryId, Pageable pageable);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.name LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR p.description LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        """)
    Page<ProductEntity> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.stock < :minStock
        AND p.active = true
        """)
    List<ProductEntity> findLowStockProducts(@Param("minStock") Integer minStock);

    long countByActiveTrue();

    @EntityGraph(attributePaths = {"orderProducts"})
    Optional<ProductEntity> findByIdWithOrderProducts(UUID id);

    @Query("""
        SELECT COUNT(op) FROM OrderProductEntity op
        WHERE op.product.id = :productId
        """)
    long countOrdersForProduct(@Param("productId") UUID productId);
}
