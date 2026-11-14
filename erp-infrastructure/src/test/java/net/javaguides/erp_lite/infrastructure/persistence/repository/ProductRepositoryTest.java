package net.javaguides.erp_lite.infrastructure.persistence.repository;

import net.javaguides.erp_lite.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Product Repository Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        testProduct = ProductEntity.builder()
                .sku("TEST-PROD-001")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("99.99"))
                .stock(100)
                .categoryId("TEST_CATEGORY")
                .imageUrl("https://example.com/image.jpg")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve product by ID")
    void testSaveAndFindById() {
        // Given
        ProductEntity saved = productRepository.save(testProduct);

        // When
        Optional<ProductEntity> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("TEST-PROD-001");
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Should find product by unique SKU")
    void testFindBySku() {
        // Given
        productRepository.save(testProduct);

        // When
        Optional<ProductEntity> found = productRepository.findBySku("TEST-PROD-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should return empty when SKU not found")
    void testFindBySkuNotFound() {
        // When
        Optional<ProductEntity> found = productRepository.findBySku("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find only active products")
    void testFindByActiveTrue() {
        // Given
        ProductEntity activeProduct = ProductEntity.builder()
                .sku("ACTIVE-001")
                .name("Active Product")
                .price(new BigDecimal("50.00"))
                .stock(10)
                .active(true)
                .build();

        ProductEntity inactiveProduct = ProductEntity.builder()
                .sku("INACTIVE-001")
                .name("Inactive Product")
                .price(new BigDecimal("50.00"))
                .stock(10)
                .active(false)
                .build();

        productRepository.save(activeProduct);
        productRepository.save(inactiveProduct);

        // When
        var activeProducts = productRepository.findByActiveTrue();

        // Then
        assertThat(activeProducts)
                .hasSize(2) // testProduct y activeProduct
                .extracting(ProductEntity::getSku)
                .contains("TEST-PROD-001", "ACTIVE-001")
                .doesNotContain("INACTIVE-001");
    }

    @Test
    @DisplayName("Should find products by category")
    void testFindByCategoryId() {
        // Given
        ProductEntity product1 = ProductEntity.builder()
                .sku("ELEC-001")
                .name("Laptop")
                .price(new BigDecimal("1299.99"))
                .stock(5)
                .categoryId("ELECTRONICS")
                .active(true)
                .build();

        ProductEntity product2 = ProductEntity.builder()
                .sku("BOOK-001")
                .name("Java Book")
                .price(new BigDecimal("39.99"))
                .stock(50)
                .categoryId("BOOKS")
                .active(true)
                .build();

        productRepository.save(product1);
        productRepository.save(product2);

        // When
        var electronics = productRepository.findByCategoryId("ELECTRONICS");
        var books = productRepository.findByCategoryId("BOOKS");

        // Then
        assertThat(electronics)
                .hasSize(1)
                .extracting(ProductEntity::getName)
                .contains("Laptop");

        assertThat(books)
                .hasSize(1)
                .extracting(ProductEntity::getName)
                .contains("Java Book");
    }

    @Test
    @DisplayName("Should find low stock products")
    void testFindLowStockProducts() {
        // Given
        ProductEntity lowStockProduct = ProductEntity.builder()
                .sku("LOW-STOCK-001")
                .name("Low Stock Item")
                .price(new BigDecimal("25.00"))
                .stock(3)
                .active(true)
                .build();

        productRepository.save(lowStockProduct);
        productRepository.save(testProduct); // stock = 100

        // When
        var lowStockItems = productRepository.findLowStockProducts(10);

        // Then
        assertThat(lowStockItems)
                .hasSize(1)
                .extracting(ProductEntity::getName)
                .contains("Low Stock Item");
    }

    @Test
    @DisplayName("Should update product")
    void testUpdateProduct() {
        // Given
        ProductEntity saved = productRepository.save(testProduct);

        // When
        saved.setPrice(new BigDecimal("149.99"));
        saved.setStock(75);
        productRepository.save(saved);

        Optional<ProductEntity> updated = productRepository.findById(saved.getId());

        // Then
        assertThat(updated).isPresent();
        assertThat(updated.get().getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        assertThat(updated.get().getStock()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should delete product")
    void testDeleteProduct() {
        // Given
        ProductEntity saved = productRepository.save(testProduct);
        UUID id = saved.getId();

        // When
        productRepository.deleteById(id);

        // Then
        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Should count active products")
    void testCountByActiveTrue() {
        // Given
        productRepository.save(testProduct);
        ProductEntity anotherProduct = ProductEntity.builder()
                .sku("ANOTHER-001")
                .name("Another Product")
                .price(new BigDecimal("49.99"))
                .stock(20)
                .active(true)
                .build();
        productRepository.save(anotherProduct);

        // When
        long count = productRepository.countByActiveTrue();

        // Then
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should have auditable timestamps")
    void testAuditableTimestamps() {
        // Given
        ProductEntity saved = productRepository.save(testProduct);

        // When
        Optional<ProductEntity> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should have version for optimistic locking")
    void testVersionField() {
        // Given
        ProductEntity saved = productRepository.save(testProduct);

        // When
        Optional<ProductEntity> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getVersion()).isNotNull();
    }
}
