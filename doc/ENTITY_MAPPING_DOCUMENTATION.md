# Mapeo JPA con Hibernate - ERP Lite

## Descripción General

Este documento describe el mapeo completo de las entidades JPA utilizando Hibernate 6.x en Spring Boot 4.x para la aplicación ERP-Lite. El mapeo está basado en el esquema PostgreSQL definido en `../db/schema.json`.

## Configuración de Dependencias

### Spring Boot 4.x + Hibernate 6.x

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.postgresql:postgresql:42.7.8'
}
```

**Notas sobre las versiones:**
- Spring Boot 4.1.2+ utiliza Hibernate 6.x por defecto
- Hibernate 6.x requiere Jakarta Persistence API (jakarta.persistence.*) en lugar de javax.persistence.*
- Se usa la generación de UUID nativa con `@UuidGenerator` de Hibernate

## Estructura de Entidades

### 1. **ProductEntity** (`products`)

**Tabla:** `public.products`

**Descripción:** Entidad que representa productos del catálogo.

**Características principales:**
- ID generado automáticamente como UUID
- SKU única y obligatoria
- Precios con precisión de 2 decimales
- Control de inventario
- Categorización flexible (categoryId es opcional)
- Auditoría (createdAt, updatedAt)
- Control de concurrencia optimista con @Version
- Relación One-to-Many con OrderProduct

**Atributos:**
| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| id | UUID | PK, NOT NULL | Generado automáticamente |
| sku | String(50) | UNIQUE, NOT NULL | Identificador de producto |
| name | String(200) | NOT NULL | Nombre del producto |
| description | TEXT | NULL | Descripción detallada |
| price | BigDecimal(15,2) | NOT NULL | Precio unitario |
| stock | Integer | NOT NULL | Cantidad en inventario |
| categoryId | String(100) | NULL | Categoría flexible |
| imageUrl | String(500) | NULL | URL de imagen |
| active | Boolean | NOT NULL | Estado del producto |
| createdAt | LocalDateTime | NOT NULL, IMMUTABLE | Timestamp de creación |
| updatedAt | LocalDateTime | NOT NULL | Timestamp de actualización |
| version | Long | Optimistic Lock | Para control de concurrencia |

**Índices:**
- `idx_product_sku` (UNIQUE) - Para búsquedas rápidas por SKU
- `idx_product_category` - Para filtrado por categoría
- `idx_product_active` - Para consultas de productos activos

**Relaciones:**
- One-to-Many con OrderProduct (a través de la tabla intermedia)

---

### 2. **OrderEntity** (`orders`)

**Tabla:** `public.orders`

**Descripción:** Entidad que representa pedidos de clientes.

**Características principales:**
- ID generado automáticamente como UUID
- Número de orden único
- Seguimiento de cliente por ID y nombre
- Gestión de estado del pedido
- Soporte multi-moneda
- Auditoría completa
- Control de concurrencia optimista
- Relación One-to-Many con OrderProduct (cascade ALL, orphanRemoval)

**Atributos:**
| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| id | UUID | PK, NOT NULL | Generado automáticamente |
| orderNumber | String(50) | UNIQUE, NOT NULL | Referencia del pedido |
| customerId | Long | NOT NULL | ID del cliente (referencia flexible) |
| customerName | String(200) | NOT NULL | Nombre del cliente |
| createdBy | String(100) | NOT NULL | Usuario que creó el pedido |
| orderDate | LocalDateTime | NOT NULL | Fecha del pedido |
| status | String(20) | NOT NULL | Estado (PENDING, PROCESSING, SHIPPED, etc.) |
| totalAmount | BigDecimal(15,2) | NOT NULL | Total del pedido |
| currency | String(3) | NOT NULL | Código de moneda (ISO 4217) |
| createdAt | LocalDateTime | NOT NULL, IMMUTABLE | Timestamp de creación |
| updatedAt | LocalDateTime | NOT NULL | Timestamp de actualización |
| version | Long | Optimistic Lock | Para control de concurrencia |

**Índices:**
- `idx_order_number` (UNIQUE) - Para búsquedas por número de pedido
- `idx_order_customer` - Para filtrado por cliente
- `idx_order_status` - Para seguimiento de estado
- `idx_order_date` - Para reportes por fecha

**Relaciones:**
- One-to-Many con OrderProduct (cascade ALL, orphanRemoval = true)

**Métodos de utilidad:**
- `addOrderProduct(OrderProductEntity)` - Agregar línea de pedido
- `removeOrderProduct(OrderProductEntity)` - Remover línea de pedido

---

### 3. **OrderProductEntity** (`order_products`)

**Tabla:** `public.order_products`

**Descripción:** Tabla intermedia que representa las líneas de un pedido (relación Many-to-Many entre Order y Product).

**Características principales:**
- Clave primaria compuesta (Order ID + Product ID)
- Implementa cálculo automático de subtotal
- Captura del nombre del producto en el momento de la compra
- Almacenamiento de precio unitario en el momento de la compra
- Relaciones bidireccionales con Order y Product
- Auditoría completa

**Atributos:**
| Atributo | Tipo | Restricciones | Notas |
|----------|------|---------------|-------|
| id | OrderProductId | PK | Clave compuesta (orderId + productId) |
| order | OrderEntity | FK, NOT NULL | Referencia al pedido |
| product | ProductEntity | FK, NOT NULL | Referencia al producto |
| productName | String(200) | NOT NULL | Nombre del producto en el momento de venta |
| quantity | Integer | NOT NULL | Cantidad de unidades |
| unitPrice | BigDecimal(15,2) | NOT NULL | Precio unitario en el momento de venta |
| subtotal | BigDecimal(15,2) | NOT NULL, COMPUTED | quantity × unitPrice |
| createdAt | LocalDateTime | NOT NULL, IMMUTABLE | Timestamp de creación |
| updatedAt | LocalDateTime | NOT NULL | Timestamp de actualización |
| version | Long | Optimistic Lock | Para control de concurrencia |

**Clave Primaria Compuesta (OrderProductId):**
```java
@Embeddable
public class OrderProductId implements Serializable {
    private UUID orderId;
    private UUID productId;
}
```

**Índices:**
- `idx_order_product_order` - Para búsquedas rápidas por pedido
- `idx_order_product_product` - Para búsquedas rápidas por producto
- `uk_order_product` (UNIQUE) - Restricción única sobre (orderId, productId)

**Relaciones:**
- Many-to-One con OrderEntity (FK: fk_order_products_order)
- Many-to-One con ProductEntity (FK: fk_order_products_product)

**Acciones de BD:**
- ON DELETE CASCADE para Order (si se elimina un pedido, se eliminan sus líneas)
- ON DELETE RESTRICT para Product (no permite eliminar un producto si está en algún pedido)

---

## Anotaciones JPA/Hibernate Utilizadas

### Anotaciones de Mapeo

| Anotación | Uso | Descripción |
|-----------|-----|-------------|
| `@Entity` | Clase | Define la clase como entidad persistible |
| `@Table` | Clase | Especifica nombre de tabla, schema e índices |
| `@Id` | Campo | Marca el campo como clave primaria |
| `@EmbeddedId` | Campo | Clave primaria compuesta |
| `@Embeddable` | Clase | Define una clase como componente embebido |
| `@Column` | Campo | Configura los detalles de la columna |
| `@JoinColumn` | Campo | Define la columna de clave foránea |
| `@OneToMany` | Campo | Relación 1 a muchos |
| `@ManyToOne` | Campo | Relación muchos a 1 |
| `@MapsId` | Campo | En entidades con clave compuesta |

### Anotaciones de Hibernate

| Anotación | Uso | Descripción |
|-----------|-----|-------------|
| `@UuidGenerator` | Campo UUID | Generación automática de UUID v4 |
| `@CreationTimestamp` | Campo LocalDateTime | Fecha de creación automática |
| `UpdateTimestamp` | Campo LocalDateTime | Fecha de actualización automática |
| `@Version` | Campo Long | Control de concurrencia optimista |

### Anotaciones de Lombok

| Anotación | Descripción |
|-----------|-------------|
| `@Getter` | Genera getters automáticamente |
| `@Setter` | Genera setters automáticamente |
| `@NoArgsConstructor` | Constructor sin argumentos |
| `@AllArgsConstructor` | Constructor con todos los argumentos |
| `@Builder` | Patrón Builder para construcción de objetos |
| `@EqualsAndHashCode` | Implementa equals() y hashCode() |

---

## Configuración de Spring Boot

### application.yaml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erp_lite
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # En producción, usar 'validate'
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
          fetch_size: 50
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
    show-sql: false
    open-in-view: false

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Restricciones de Integridad Referencial

### Foreign Keys

1. **fk_order_products_order**
   - Tabla: order_products
   - Columna: order_id
   - Referencia: orders.id
   - ON DELETE: CASCADE
   - ON UPDATE: NO ACTION

2. **fk_order_products_product**
   - Tabla: order_products
   - Columna: product_id
   - Referencia: products.id
   - ON DELETE: RESTRICT
   - ON UPDATE: NO ACTION

### Restricciones Únicas

1. **products.sku** - UNIQUE
2. **orders.order_number** - UNIQUE
3. **order_products (order_id, product_id)** - UNIQUE

---

## Estrategia de Generación de IDs

### UUID Tipo 4 (Aleatorio)

Todas las entidades principales (Product, Order) utilizan UUID v4 para sus claves primarias:

```java
@Id
@UuidGenerator(style = GenerationType.AUTO)
@Column(name = "id", nullable = false, updatable = false)
private UUID id;
```

**Ventajas:**
- Generación distribuida (sin depender de secuencias de BD)
- Escalabilidad en múltiples instancias
- Privacidad (IDs no secuenciales no exponen información)
- Portabilidad entre bases de datos

---

## Control de Concurrencia

### Optimistic Locking

Se utiliza `@Version` en todas las entidades para implementar Optimistic Locking:

```java
@Version
private Long version;
```

**Comportamiento:**
- Hibernate incrementa automáticamente la versión en cada actualización
- Si dos transacciones modifican el mismo registro, una fallará con `OptimisticLockException`
- Ideal para sistemas de baja concurrencia o sin requisitos de "last write wins"

---

## Auditoría y Timestamps

### Timestamps Automáticos

Se utilizan anotaciones de Hibernate para auditoría automática:

```java
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
```

**Características:**
- `createdAt` se establece una sola vez al crear el registro
- `updatedAt` se actualiza automáticamente en cada modificación
- Usa el servidor de BD para garantizar consistencia

---

## Relaciones y Cascadas

### Order → OrderProduct

```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
)
```

**Comportamiento:**
- LAZY: Las líneas de pedido se cargan bajo demanda
- CASCADE ALL: Al guardar/actualizar/eliminar una Order, se propaga a sus OrderProducts
- orphanRemoval = true: Los OrderProducts huérfanos se eliminan automáticamente
- Permite gestión completa del ciclo de vida desde Order

### Product → OrderProduct

```java
@OneToMany(
    mappedBy = "product",
    cascade = CascadeType.REFRESH,
    fetch = FetchType.LAZY,
    orphanRemoval = false
)
```

**Comportamiento:**
- REFRESH: Solo refresca los datos sin propagar cambios
- Protege los productos de eliminación accidental
- Los OrderProducts no se eliminan con el Product

### OrderProduct → Order/Product

```java
@ManyToOne(
    fetch = FetchType.LAZY,
    cascade = CascadeType.MERGE,
    optional = false
)
```

**Comportamiento:**
- LAZY: Carga eficiente de relaciones
- MERGE: Propaga cambios de estado merged
- optional = false: Las relaciones son obligatorias

---

## Estrategia de Carga (Fetch Strategy)

| Relación | Estrategia | Justificación |
|----------|-----------|---------------|
| Order → OrderProducts | LAZY | Evita cargar todas las líneas innecesariamente |
| Product → OrderProducts | LAZY | Evita cargar todas las órdenes de un producto |
| OrderProduct → Order | LAZY | Permite carga selectiva del contexto |
| OrderProduct → Product | LAZY | Permite carga selectiva del contexto |

**Recomendación:** Usar Graph API de JPA para consultas que requieren cargar relaciones:

```java
@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
public Order findByIdWithDetails(UUID id);
```

---

## Validación de Datos

### Anotaciones Jakarta Validation

Se recomienda agregar validaciones en las entidades:

```java
import jakarta.validation.constraints.*;

@Column(name = "sku", nullable = false)
@NotBlank(message = "SKU es requerido")
@Size(min = 1, max = 50)
private String sku;

@Column(name = "price", nullable = false)
@DecimalMin("0.01")
private BigDecimal price;

@Column(name = "stock", nullable = false)
@Min(0)
private Integer stock;
```

---

## Performance y Optimizaciones

### Query Optimization

1. **Use @EntityGraph para evitar N+1 queries:**
   ```java
   @EntityGraph(attributePaths = {"orderProducts"})
   List<Order> findAllWithProducts();
   ```

2. **Use projections para queries de solo lectura:**
   ```java
   public interface OrderSummary {
       UUID getId();
       String getOrderNumber();
       BigDecimal getTotalAmount();
   }
   ```

3. **Batch operations:**
   ```yaml
   hibernate.jdbc.batch_size: 20
   hibernate.order_inserts: true
   hibernate.order_updates: true
   ```

4. **Índices en columnas frecuentemente consultadas:**
   - Todas las claves foráneas
   - Columnas con filtros frecuentes (status, active)
   - Columnas de búsqueda (sku, orderNumber)

---

## Manejo de Transacciones

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    readOnly = false,
    timeout = 30
)
public void createOrderWithProducts(Order order, List<OrderProduct> products) {
    orderRepository.save(order);
    orderProductRepository.saveAll(products);
}
```

---

## Migraciones de Base de Datos

Se utiliza Flyway o Liquibase para versionamiento. Archivos de esquema:
- `/db/postgresql/01-schema.sql` - Definición de tablas
- `/db/postgresql/02-data.sql` - Datos iniciales

La configuración DDL debe ser:
```yaml
spring.jpa.hibernate.ddl-auto=validate
```

En desarrollo se puede usar `create-drop` o `update`, pero en producción **siempre usar `validate`**.

---

## Ejemplo de Uso

```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Order createOrder(OrderCreateRequest request) {
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .customerId(request.getCustomerId())
            .customerName(request.getCustomerName())
            .createdBy(getCurrentUser())
            .orderDate(LocalDateTime.now())
            .status("PENDING")
            .totalAmount(BigDecimal.ZERO)
            .currency("USD")
            .build();

        BigDecimal total = BigDecimal.ZERO;
        
        for (OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException());

            OrderProduct orderProduct = OrderProduct.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .quantity(item.getQuantity())
                .unitPrice(product.getPrice())
                .build();
            // El subtotal se calcula automáticamente en @PrePersist

            order.addOrderProduct(orderProduct);
            total = total.add(orderProduct.getSubtotal());
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }
}
```

---

## Referencias

- [Hibernate 6.x Documentation](https://hibernate.org/orm/documentation/)
- [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Dialect in Hibernate](https://hibernate.org/orm/documentation/latest/userguide/#database-dialect)
