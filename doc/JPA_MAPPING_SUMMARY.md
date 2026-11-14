# 📊 Mapeo de Entidades JPA - ERP Lite

## ✅ Completado

Se ha creado un mapeo completo de entidades JPA utilizando **Hibernate 6.x** con **Spring Boot 4.1.2** basado en el esquema PostgreSQL de la base de datos ERP.

### 🎯 Entidades Implementadas

#### 1. **ProductEntity** 
- **Tabla:** `public.products`
- **Descripción:** Gestión del catálogo de productos
- **Características:**
  - UUID generado automáticamente con `@UuidGenerator`
  - SKU única obligatoria
  - Precios con precisión 15,2 (BigDecimal)
  - Control de inventario
  - Categorización flexible
  - Auditoría completa (createdAt, updatedAt)
  - Versionado optimista (`@Version`)
  - Relación One-to-Many con OrderProduct

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/ProductEntity.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/ProductEntity.java)

---

#### 2. **OrderEntity**
- **Tabla:** `public.orders`
- **Descripción:** Gestión de pedidos de clientes
- **Características:**
  - UUID generado automáticamente
  - Número de orden único
  - Tracking de cliente (ID y nombre)
  - Gestión de estados (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
  - Soporte multi-moneda (USD, EUR, etc.)
  - Auditoría completa
  - Versionado optimista
  - Relación One-to-Many con OrderProduct (cascade ALL, orphanRemoval = true)
  - Métodos helper: `addOrderProduct()`, `removeOrderProduct()`

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderEntity.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderEntity.java)

---

#### 3. **OrderProductEntity**
- **Tabla:** `public.order_products`
- **Descripción:** Tabla intermedia - líneas de pedido
- **Características:**
  - Clave primaria compuesta (OrderProductId)
  - Cálculo automático de subtotal en `@PrePersist`
  - Captura de precio en el momento de la venta
  - Auditoría automática
  - Relaciones Many-to-One bidireccionales

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderProductEntity.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderProductEntity.java)

---

#### 4. **OrderProductId**
- **Descripción:** Clave primaria compuesta como `@Embeddable`
- **Atributos:** 
  - orderId (UUID)
  - productId (UUID)
- **Implementa:** Serializable, equals(), hashCode()

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderProductId.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/OrderProductId.java)

---

#### 5. **BaseAuditEntity**
- **Descripción:** Clase base para auditoría
- **MappedSuperclass** que proporciona:
  - `createdAt` - inmutable
  - `updatedAt` - se actualiza automáticamente
  - `version` - para optimistic locking

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/BaseAuditEntity.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/entity/BaseAuditEntity.java)

---

### 📚 Repositorios Spring Data JPA

#### ProductRepository
- Búsqueda por SKU
- Productos activos
- Búsqueda full-text
- Productos con bajo stock
- Carga eager con EntityGraph

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/ProductRepository.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/ProductRepository.java)

---

#### OrderRepository
- Búsqueda por número de orden
- Órdenes por cliente
- Filtrado por estado
- Consultas con EntityGraph
- Rango de fechas
- Cálculo de ingresos

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/OrderRepository.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/OrderRepository.java)

---

#### OrderProductRepository
- Líneas por orden
- Órdenes que contienen un producto
- Cantidad total vendida por producto

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/OrderProductRepository.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/persistence/repository/OrderProductRepository.java)

---

### ⚙️ Servicio de Negocio

**OrderManagementService**
- Crear órdenes con validación de inventario
- Agregar/remover productos de órdenes
- Cambio de estado con validación
- Cálculo de totales

**Ubicación:** [`../erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/service/OrderManagementService.java`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/service/OrderManagementService.java)

---

### 🔧 Configuración de Hibernate

**Archivo:** [`../erp-api/src/main/resources/application.yaml`](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/erp-api/src/main/resources/application.yaml)

**Configuraciones clave:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # En producción, usar 'validate'
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
```

---

### 📋 Anotaciones Utilizadas

#### Jakarta Persistence (JPA)
- `@Entity` - Define la clase como persistible
- `@Table` - Configura tabla, schema e índices
- `@Id` - Clave primaria
- `@EmbeddedId` - Clave compuesta
- `@Embeddable` - Componente embebido
- `@Column` - Configuración de columna
- `@JoinColumn` - Configuración de clave foránea
- `@OneToMany` / `@ManyToOne` - Relaciones
- `@MapsId` - Mapeo de clave compuesta
- `@ForeignKey` - Definición de FK
- `@Index` - Índices de BD
- `@UniqueConstraint` - Restricciones únicas

#### Hibernate
- `@UuidGenerator` - Generación automática de UUID v4
- `@CreationTimestamp` - Fecha de creación automática
- `@UpdateTimestamp` - Fecha de actualización automática
- `@Version` - Optimistic locking
- `@PrePersist` / `@PreUpdate` - Callbacks de ciclo de vida

#### Lombok
- `@Getter` / `@Setter` - Accesores automáticos
- `@NoArgsConstructor` - Constructor sin argumentos
- `@AllArgsConstructor` - Constructor con todos los argumentos
- `@Builder` - Patrón Builder
- `@EqualsAndHashCode` - Implementación de equals/hashCode
- `@RequiredArgsConstructor` - Constructor con campos final

---

## 🚀 Cómo Usar

### 1. Crear un Producto
```java
@Autowired
private ProductRepository productRepository;

ProductEntity product = ProductEntity.builder()
    .sku("LAPTOP-001")
    .name("Dell XPS 13")
    .price(new BigDecimal("1299.99"))
    .stock(50)
    .categoryId("ELECTRONICS")
    .active(true)
    .build();

productRepository.save(product);
```

### 2. Crear una Orden
```java
@Autowired
private OrderManagementService orderService;

List<OrderItemRequest> items = List.of(
    new OrderItemRequest(productId1, 2),
    new OrderItemRequest(productId2, 1)
);

OrderEntity order = orderService.createOrder(
    "ORD-20241114-001",
    123L,
    "Juan García",
    "admin",
    items
);
```

### 3. Consultar con EntityGraph
```java
// Evita N+1 queries
OrderEntity order = orderRepository
    .findByIdWithDetails(orderId)
    .get();

// orderProducts ya está cargado
for (OrderProductEntity op : order.getOrderProducts()) {
    System.out.println(op.getProductName());
}
```

---

## 📊 Restricciones de Integridad

| Restricción | Tabla | Columnas | Referencia | Acción |
|-----------|-------|----------|-----------|--------|
| fk_order_products_order | order_products | order_id | orders.id | CASCADE |
| fk_order_products_product | order_products | product_id | products.id | RESTRICT |
| uk_product_sku | products | sku | - | UNIQUE |
| uk_order_number | orders | order_number | - | UNIQUE |
| uk_order_product | order_products | (order_id, product_id) | - | UNIQUE |

---

## 🔍 Índices Creados

### Tabla Products
- `idx_product_sku` (UNIQUE)
- `idx_product_category`
- `idx_product_active`

### Tabla Orders
- `idx_order_number` (UNIQUE)
- `idx_order_customer`
- `idx_order_status`
- `idx_order_date`

### Tabla OrderProducts
- `idx_order_product_order`
- `idx_order_product_product`

---

## 📝 Documentación Completa

Para documentación detallada, consulta:

1. **[ENTITY_MAPPING_DOCUMENTATION.md](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/ENTITY_MAPPING_DOCUMENTATION.md)**
   - Explicación completa de cada entidad
   - Anotaciones utilizadas
   - Configuración de Hibernate
   - Relaciones y cascadas
   - Performance y optimizaciones

2. **[JPA_IMPLEMENTATION_GUIDE.md](/Volumes/HomeX/DEV/git/courses/springboot-ddd/erp-lite/JPA_IMPLEMENTATION_GUIDE.md)**
   - Guía de inicio rápido
   - Configuración de BD
   - Ejemplos de uso
   - Testing
   - Troubleshooting

---

## ✅ Status de Compilación

```
BUILD SUCCESSFUL in 1s
17 actionable tasks: 17 executed
```

Todas las entidades, repositorios y servicios compilan exitosamente sin errores.

---

## 📂 Estructura de Archivos

```
erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/
├── persistence/
│   ├── entity/
│   │   ├── BaseAuditEntity.java
│   │   ├── ProductEntity.java
│   │   ├── OrderEntity.java
│   │   ├── OrderProductEntity.java
│   │   └── OrderProductId.java
│   └── repository/
│       ├── ProductRepository.java
│       ├── OrderRepository.java
│       └── OrderProductRepository.java
└── service/
    ├── OrderManagementService.java
    └── OrderItemRequest.java
```

---

## 🔗 Dependencias Utilizadas

- **Spring Boot 4.1.2**
- **Hibernate 6.x** (incluido en Spring Boot)
- **Jakarta Persistence API**
- **PostgreSQL Driver 42.7.8**
- **Spring Data JPA**
- **Lombok**
- **MapStruct**

---

## 📞 Próximos Pasos

1. **Crear Controllers REST** para exponer los servicios
2. **Implementar Mappers** con MapStruct para DTOs
3. **Agregar Validaciones** con Jakarta Validation
4. **Crear Tests** de repositorios y servicios
5. **Implementar Auditoría** con Spring Security
6. **Configurar Paginación** en consultas
7. **Implementar Soft Delete** si es necesario
8. **Agregar Caching** con Spring Cache

---

## 📚 Referencias

- [Hibernate 6.x Documentation](https://hibernate.org/orm/documentation/)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/)
- [PostgreSQL Dialect Hibernate](https://hibernate.org/orm/documentation/latest/userguide/#database-dialect)

---

**Creado con:** Copilot SDK en VS Code  
**Fecha:** 2024-11-14  
**Versión:** 1.0
