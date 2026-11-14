# 🎯 Mapeo de Entidades JPA - ERP Lite

**Status:** ✅ Completado y verificado  
**Versión:** 1.0  
**Última actualización:** 14 Noviembre 2024

---

## 📋 Resumen Ejecutivo

Se ha creado un **mapeo completo de entidades JPA** utilizando **Hibernate 6.x** con **Spring Boot 4.1.2**, basado exactamente en el esquema PostgreSQL proporcionado. El mapeo incluye:

- ✅ **5 Entidades JPA** completamente mapeadas
- ✅ **3 Repositorios** Spring Data JPA con consultas avanzadas
- ✅ **1 Servicio** de negocio con lógica transaccional
- ✅ **16 Archivos** de código + documentación
- ✅ **Compilación exitosa** sin errores

---

## 🚀 Inicio Rápido (5 minutos)

### 1. Configurar Base de Datos

```bash
# Crear BD
createdb erp_lite

# Ejecutar esquema
psql -U postgres -d erp_lite -f db/postgresql/01-schema.sql
psql -U postgres -d erp_lite -f db/postgresql/02-data.sql
```

### 2. Actualizar Conexión (si es necesario)

Editar `../erp-api/src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erp_lite
    username: postgres
    password: tu_contraseña
```

### 3. Compilar y Ejecutar

```bash
./gradlew clean build
./gradlew bootRun
```

---

## 📚 Documentación Completa

| Documento | Descripción | Tiempo de lectura |
|-----------|-------------|------------------|
| **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** | **ÍNDICE COMPLETO** - Comienza aquí | 5 min |
| [JPA_IMPLEMENTATION_GUIDE.md](JPA_IMPLEMENTATION_GUIDE.md) | Guía práctica paso a paso | 10 min |
| [ENTITY_MAPPING_DOCUMENTATION.md](ENTITY_MAPPING_DOCUMENTATION.md) | Referencia técnica detallada | 20 min |
| [ENTITY_RELATIONSHIPS_DIAGRAM.md](ENTITY_RELATIONSHIPS_DIAGRAM.md) | Diagramas y decisiones de diseño | 15 min |
| [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md) | 15+ ejemplos prácticos | 20 min |
| [JPA_MAPPING_SUMMARY.md](JPA_MAPPING_SUMMARY.md) | Resumen ejecutivo | 5 min |
| [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md) | Verificación de completitud | 5 min |

---

## 📦 Entidades Creadas

### 1. ProductEntity (Tabla: products)
Gestiona el catálogo de productos con control de inventario.

**Características:**
- UUID autogenerado
- SKU única y obligatoria
- Precios con precisión 15,2 (BigDecimal)
- Control de inventario
- Categorización flexible
- Auditoría automática (createdAt, updatedAt)
- Versionado optimista (@Version)

**Ubicación:** `erp-infrastructure/persistence/entity/ProductEntity.java`

---

### 2. OrderEntity (Tabla: orders)
Gestiona pedidos de clientes con múltiples estados.

**Características:**
- UUID autogenerado
- Número de orden único
- Seguimiento por cliente (ID y nombre)
- Gestión de estados (PENDING, PROCESSING, SHIPPED, DELIVERED)
- Soporte multi-moneda (USD, EUR, etc.)
- Relación One-to-Many con OrderProduct (cascade ALL)
- Auditoría completa

**Ubicación:** `erp-infrastructure/persistence/entity/OrderEntity.java`

---

### 3. OrderProductEntity (Tabla: order_products)
Tabla intermedia que representa las líneas de un pedido.

**Características:**
- Clave primaria compuesta (Order ID + Product ID)
- Cálculo automático de subtotal
- Captura de precio en el momento de la venta
- Auditoría automática
- Relaciones Many-to-One bidireccionales

**Ubicación:** `erp-infrastructure/persistence/entity/OrderProductEntity.java`

---

### Entidades Adicionales

**OrderProductId.java** - Clave compuesta @Embeddable  
**BaseAuditEntity.java** - Clase base para auditoría (@MappedSuperclass)

---

## 📚 Repositorios Disponibles

### ProductRepository
```java
// Búsqueda
Optional<ProductEntity> findBySku(String sku);
List<ProductEntity> findByActiveTrue();
List<ProductEntity> findByCategoryId(String categoryId);

// Búsqueda avanzada
Page<ProductEntity> searchProducts(String searchTerm, Pageable pageable);
List<ProductEntity> findLowStockProducts(Integer minStock);
```

### OrderRepository
```java
// Búsqueda
Optional<OrderEntity> findByOrderNumber(String orderNumber);
Page<OrderEntity> findByCustomerId(Long customerId, Pageable pageable);
List<OrderEntity> findByStatus(String status);

// Consultas avanzadas
Page<OrderEntity> findOrdersByDateRange(LocalDateTime start, LocalDateTime end, Pageable page);
BigDecimal calculateTotalRevenue(LocalDateTime fromDate);
```

### OrderProductRepository
```java
List<OrderProductEntity> findByOrderId(UUID orderId);
long countOrdersContainingProduct(UUID productId);
Integer getTotalQuantitySoldForProduct(UUID productId);
```

---

## ⚙️ Servicio de Negocio

### OrderManagementService

```java
// Crear orden con validación de inventario
OrderEntity createOrder(String orderNumber, Long customerId, 
                        String customerName, String createdBy, 
                        List<OrderItemRequest> items);

// Agregar/remover productos
void addProductToOrder(UUID orderId, UUID productId, Integer quantity);
void removeProductFromOrder(UUID orderId, UUID productId);

// Gestión de estado
OrderEntity updateOrderStatus(UUID orderId, String newStatus);

// Consultas
OrderEntity getOrderWithDetails(UUID orderId);
BigDecimal calculateOrderTotal(UUID orderId);
```

---

## 📊 Ejemplo de Uso

### Crear una Orden

```java
@Autowired
private OrderManagementService orderService;

// Preparar items
List<OrderItemRequest> items = List.of(
    new OrderItemRequest(productId1, 2),
    new OrderItemRequest(productId2, 1)
);

// Crear orden
try {
    OrderEntity order = orderService.createOrder(
        "ORD-20241114-001",
        123L,
        "Juan García",
        "admin",
        items
    );
    System.out.println("Total: " + order.getTotalAmount());
} catch (IllegalArgumentException e) {
    System.out.println("Error: " + e.getMessage());
}
```

### Consultar Orden con Detalles

```java
@Autowired
private OrderRepository orderRepository;

// Obtener con EntityGraph para evitar N+1 queries
OrderEntity order = orderRepository
    .findByIdWithDetails(orderId)
    .get();

// Acceder a líneas sin queries adicionales
order.getOrderProducts().forEach(lineItem ->
    System.out.println(lineItem.getProductName() + ": " + lineItem.getSubtotal())
);
```

---

## 🔗 Relaciones Implementadas

```
Product  1 ──M── OrderProduct ──1── Order

• Product → OrderProduct: One-to-Many (REFRESH, orphanRemoval = false)
• Order → OrderProduct: One-to-Many (CASCADE ALL, orphanRemoval = true)
• Foreign Keys: CASCADE para Order, RESTRICT para Product
```

---

## 📊 Características Implementadas

✅ **UUID v4** generados automáticamente con `@UuidGenerator`  
✅ **Auditoría** automática (createdAt, updatedAt)  
✅ **Optimistic Locking** con `@Version`  
✅ **Índices** para optimización de queries  
✅ **Restricciones** de integridad referencial  
✅ **Cascadas inteligentes** (CASCADE, REFRESH, RESTRICT)  
✅ **Relaciones bidireccionales** correctamente mapeadas  
✅ **Lazy loading** con EntityGraph para evitar N+1  
✅ **Cálculo automático** de subtotales  
✅ **Soporte multi-moneda**  
✅ **Validaciones** de negocio  
✅ **Transacciones** declarativas  

---

## ⚙️ Configuración Hibernate

La configuración está en `../erp-api/src/main/resources/application.yaml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        jdbc.batch_size: 20
        order_inserts: true
```

---

## ✅ Estado de Compilación

```
BUILD SUCCESSFUL in 1s
17 actionable tasks: 17 executed

✓ Todas las entidades compilan
✓ Todos los repositorios compilan
✓ Todos los servicios compilan
✓ Tests compilan correctamente
✓ Sin errores ni advertencias críticas
```

---

## 🎯 Próximos Pasos

- [ ] Crear controladores REST
- [ ] Mapear DTOs con MapStruct
- [ ] Agregar validaciones Jakarta Validation
- [ ] Escribir tests de integración
- [ ] Implementar Spring Security
- [ ] Agregar más índices si es necesario
- [ ] Configurar caching

---

## 📁 Estructura del Proyecto

```
erp-infrastructure/src/main/java/net/javaguides/erp_lite/infrastructure/
├── persistence/
│   ├── entity/              (5 archivos)
│   │   ├── BaseAuditEntity.java
│   │   ├── ProductEntity.java
│   │   ├── OrderEntity.java
│   │   ├── OrderProductEntity.java
│   │   └── OrderProductId.java
│   └── repository/          (3 archivos)
│       ├── ProductRepository.java
│       ├── OrderRepository.java
│       └── OrderProductRepository.java
└── service/                 (2 archivos)
    ├── OrderManagementService.java
    └── OrderItemRequest.java

Raíz del proyecto/
├── DOCUMENTATION_INDEX.md        (Índice de docs)
├── JPA_IMPLEMENTATION_GUIDE.md   (Guía práctica)
├── ENTITY_MAPPING_DOCUMENTATION.md (Referencia)
├── ENTITY_RELATIONSHIPS_DIAGRAM.md (Diseño)
├── USAGE_EXAMPLES.md            (Ejemplos)
├── JPA_MAPPING_SUMMARY.md       (Resumen)
└── COMPLETION_CHECKLIST.md      (Verificación)
```

---

## 🔍 Buscar en la Documentación

### "¿Cómo creo...?"
→ Ve a [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)

### "¿Cuál es la diferencia entre...?"
→ Ve a [ENTITY_RELATIONSHIPS_DIAGRAM.md](ENTITY_RELATIONSHIPS_DIAGRAM.md)

### "¿Qué significa esta anotación?"
→ Ve a [ENTITY_MAPPING_DOCUMENTATION.md](ENTITY_MAPPING_DOCUMENTATION.md)

### "¿Cómo optimizo mis queries?"
→ Ve a [ENTITY_MAPPING_DOCUMENTATION.md](ENTITY_MAPPING_DOCUMENTATION.md) § Performance

### "No puedo compilar"
→ Ve a [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md) § Build Verification

---

## 📞 Soporte

1. **Documentación completa:** [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)
2. **Ejemplos prácticos:** [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
3. **Troubleshooting:** [JPA_IMPLEMENTATION_GUIDE.md](JPA_IMPLEMENTATION_GUIDE.md) § Troubleshooting
4. **Referencias:** 
   - [Hibernate 6.x Documentation](https://hibernate.org/orm/documentation/)
   - [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
   - [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/)

---

## 📄 Licencia y Atribuciones

Creado con **Copilot SDK en VS Code**  
**Tecnologías:** Spring Boot 4.1.2, Hibernate 6.x, PostgreSQL, Jakarta API  
**Fecha:** 14 Noviembre 2024

---

**¡Mapeo JPA completado y listo para usar!** 🚀
