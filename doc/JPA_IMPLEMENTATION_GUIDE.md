# JPA Entity Mapping - Guía de Implementación

## 📋 Resumen

Este proyecto implementa un mapeo completo de entidades JPA usando Hibernate 6.x con Spring Boot 4.x. Las entidades están diseñadas para gestionar un sistema ERP con órdenes y productos.

## 🗂️ Estructura de Archivos

```
erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/
├── entity/
│   ├── BaseAuditEntity.java              # Clase base para entidades con auditoría
│   ├── ProductEntity.java                # Entidad de productos
│   ├── OrderEntity.java                  # Entidad de pedidos
│   ├── OrderProductEntity.java           # Tabla intermedia orden-producto
│   └── OrderProductId.java               # Clave compuesta para OrderProduct
└── repository/
    ├── ProductRepository.java            # Repositorio de productos
    ├── OrderRepository.java              # Repositorio de pedidos
    └── OrderProductRepository.java       # Repositorio de líneas de pedido
```

## 🚀 Inicio Rápido

### 1. Configuración de Base de Datos PostgreSQL

Antes de ejecutar la aplicación, asegúrate de tener PostgreSQL ejecutándose y las tablas creadas:

```bash
# Conectar a PostgreSQL
psql -U postgres

# Crear base de datos
CREATE DATABASE erp_lite;

# Ejecutar el script de esquema
psql -U postgres -d erp_lite -f db/postgresql/01-schema.sql
psql -U postgres -d erp_lite -f db/postgresql/02-data.sql
```

### 2. Configurar Conexión en application.yaml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erp_lite
    username: postgres
    password: <tu-contraseña>
```

### 3. Compilar y Ejecutar

```bash
./gradlew clean build
./gradlew bootRun
```

## 📝 Descripción de Entidades

### ProductEntity
Representa un producto en el catálogo.

**Características:**
- ID: UUID (generado automáticamente)
- SKU único
- Precio con precisión de 2 decimales
- Control de inventario
- Categorización flexible
- Auditoría (createdAt, updatedAt)
- Versionado optimista

**Ejemplo de uso:**
```java
Product product = Product.builder()
    .sku("PROD-001")
    .name("Laptop Dell XPS")
    .description("High-performance laptop")
    .price(new BigDecimal("1299.99"))
    .stock(50)
    .categoryId("ELECTRONICS")
    .active(true)
    .build();

productRepository.save(product);
```

### OrderEntity
Representa un pedido de cliente.

**Características:**
- ID: UUID (generado automáticamente)
- Número de orden único
- Seguimiento por cliente
- Gestión de estado
- Soporte multi-moneda
- Auditoría completa
- Relación One-to-Many con OrderProduct

**Estados válidos:** PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

**Ejemplo de uso:**
```java
Order order = Order.builder()
    .orderNumber("ORD-001")
    .customerId(1L)
    .customerName("Juan García")
    .createdBy("admin")
    .orderDate(LocalDateTime.now())
    .status("PENDING")
    .totalAmount(new BigDecimal("1299.99"))
    .currency("USD")
    .build();

orderRepository.save(order);
```

### OrderProductEntity
Tabla intermedia que representa las líneas de un pedido.

**Características:**
- Clave primaria compuesta (orderId + productId)
- Cálculo automático de subtotal
- Captura de precio en el momento de la venta
- Auditoría automática

**Ejemplo de uso:**
```java
OrderProduct lineItem = OrderProduct.builder()
    .id(new OrderProductId(orderId, productId))
    .order(order)
    .product(product)
    .productName(product.getName())
    .quantity(2)
    .unitPrice(product.getPrice())
    .build();
// El subtotal se calcula automáticamente en @PrePersist

order.addOrderProduct(lineItem);
orderProductRepository.save(lineItem);
```

## 🔍 Consultas Disponibles

### ProductRepository

```java
// Buscar por SKU
Optional<ProductEntity> findBySku("PROD-001");

// Productos activos
List<ProductEntity> findByActiveTrue();

// Búsqueda full-text
Page<ProductEntity> searchProducts("laptop", pageable);

// Productos con bajo stock
List<ProductEntity> findLowStockProducts(10);

// Productos por categoría
Page<ProductEntity> findByCategoryId("ELECTRONICS", pageable);
```

### OrderRepository

```java
// Buscar por número de orden
Optional<OrderEntity> findByOrderNumber("ORD-001");

// Órdenes de un cliente
Page<OrderEntity> findByCustomerId(1L, pageable);

// Órdenes por estado
Page<OrderEntity> findByStatus("PENDING", pageable);

// Órdenes con detalles (carga eager)
Optional<OrderEntity> findByIdWithDetails(orderId);

// Órdenes en un rango de fechas
Page<OrderEntity> findOrdersByDateRange(startDate, endDate, pageable);

// Ingresos totales
BigDecimal calculateTotalRevenue(LocalDateTime.now().minusMonths(1));
```

### OrderProductRepository

```java
// Líneas de una orden
List<OrderProductEntity> findByOrderId(orderId);

// Órdenes que contienen un producto
long countOrdersContainingProduct(productId);

// Cantidad total vendida de un producto
Integer getTotalQuantitySoldForProduct(productId);
```

## 🔐 Configuración de Seguridad

### Validación de Entidades

Se recomienda agregar validaciones Jakarta Validation:

```java
import jakarta.validation.constraints.*;

@Entity
public class ProductEntity {
    @NotBlank(message = "SKU es requerido")
    @Size(min = 1, max = 50)
    private String sku;

    @NotNull(message = "Precio es requerido")
    @DecimalMin("0.01")
    private BigDecimal price;

    @Min(value = 0, message = "Stock no puede ser negativo")
    private Integer stock;
}
```

### Transacciones

```java
@Transactional
public void createOrderWithProducts(Order order, List<OrderProduct> products) {
    orderRepository.save(order);
    orderProductRepository.saveAll(products);
    // Ambas operaciones se ejecutan en la misma transacción
    // Si una falla, toda la transacción se revierte
}
```

## ⚡ Optimizaciones de Performance

### 1. Evitar N+1 Queries

**❌ Problema:**
```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    for (OrderProduct op : order.getOrderProducts()) { // N+1 queries
        System.out.println(op.getProductName());
    }
}
```

**✅ Solución:**
```java
@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
Optional<OrderEntity> findByIdWithDetails(UUID id);

Order order = orderRepository.findByIdWithDetails(id).get();
for (OrderProduct op : order.getOrderProducts()) {
    System.out.println(op.getProductName()); // Sin queries adicionales
}
```

### 2. Usar Proyecciones para Queries de Solo Lectura

```java
public interface OrderSummary {
    UUID getId();
    String getOrderNumber();
    BigDecimal getTotalAmount();
}

@Query("SELECT new map(o.id as id, o.orderNumber as orderNumber, o.totalAmount as totalAmount) "
       + "FROM OrderEntity o WHERE o.status = ?1")
List<Map<String, Object>> getOrderSummaries(String status);
```

### 3. Batch Operations

La configuración de Hibernate en `application.yaml` ya incluye:
```yaml
hibernate.jdbc.batch_size: 20
hibernate.order_inserts: true
hibernate.order_updates: true
```

### 4. Índices

Las entidades ya incluyen índices para:
- Búsquedas por clave (SKU, orderNumber)
- Filtrado (status, active, categoryId)
- Ordenamiento (orderDate, createdAt)

## 🧪 Testing

### Test de Repositorio

```java
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testFindBySku() {
        ProductEntity product = ProductEntity.builder()
            .sku("PROD-001")
            .name("Test Product")
            .price(new BigDecimal("99.99"))
            .stock(10)
            .active(true)
            .build();

        productRepository.save(product);
        
        Optional<ProductEntity> found = productRepository.findBySku("PROD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }
}
```

### Test de Transacciones

```java
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testOrderCreationWithRollback() {
        Order order = createTestOrder();
        orderRepository.save(order);
        
        // Al final del test, la transacción se revierte
        // Los datos no persisten en la BD
    }
}
```

## 📊 Monitoreo de Queries

Para ver las queries generadas por Hibernate:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Esto mostrará:
- Queries SQL
- Parámetros vinculados
- Tiempo de ejecución

## 🔄 Migraciones

Las migraciones de base de datos se gestionan en:
- `../db/postgresql/01-schema.sql` - Esquema inicial
- `../db/postgresql/02-data.sql` - Datos de prueba

Para actualizar el esquema en producción:
1. Crear un nuevo archivo de migración
2. Aplicar cambios con Flyway o Liquibase
3. Actualizar las entidades JPA en consecuencia

## ⚙️ Configuración de Producción

Para producción, usar estas configuraciones:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Nunca auto-update en producción
    properties:
      hibernate:
        jdbc.batch_size: 50
        jdbc.fetch_size: 100
        enable_lazy_load_no_trans: false

logging:
  level:
    org.hibernate.SQL: INFO
    org.hibernate.type.descriptor.sql.BasicBinder: INFO
```

## 📚 Referencias Adicionales

- [Hibernate 6.x Documentation](https://hibernate.org/orm/documentation/)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/)
- [PostgreSQL Dialect Hibernate](https://hibernate.org/orm/documentation/latest/userguide/#database-dialect)

## 🐛 Troubleshooting

### Problema: `no such table or schema`

**Causa:** Las tablas no existen en la base de datos.

**Solución:**
```bash
psql -U postgres -d erp_lite -f db/postgresql/01-schema.sql
```

### Problema: `could not obtain connection metadata`

**Causa:** No hay conexión con la base de datos.

**Solución:** Verificar credenciales y que PostgreSQL esté ejecutándose:
```bash
# Verificar conexión
psql -U postgres -h localhost -d erp_lite -c "SELECT 1"
```

### Problema: `OptimisticLockException`

**Causa:** Dos transacciones modifican el mismo registro simultáneamente.

**Solución:** Implementar reintentos o usar pesimistic locking:
```java
@Transactional
public void updateOrder(UUID id, OrderUpdateRequest request) {
    Order order = orderRepository.findById(id).orElseThrow();
    // Otra transacción también modifica este orden
    // Falla con OptimisticLockException
}
```

## 📞 Soporte

Para dudas o problemas con las entidades JPA, revisar:
1. [ENTITY_MAPPING_DOCUMENTATION.md](ENTITY_MAPPING_DOCUMENTATION.md)
2. Log de Hibernate en consola
3. Documentación oficial de Hibernate y Spring Data JPA
