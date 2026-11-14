# Ejemplos de Uso - API JPA

## 🚀 Ejemplos Prácticos de Uso de las Entidades JPA

### 1. Crear un Producto

```java
@Autowired
private ProductRepository productRepository;

// Crear un nuevo producto
ProductEntity laptop = ProductEntity.builder()
    .sku("LAPTOP-DELL-001")
    .name("Dell XPS 13")
    .description("High-performance ultrabook laptop with 13-inch FHD display")
    .price(new BigDecimal("1299.99"))
    .stock(50)
    .categoryId("ELECTRONICS")
    .imageUrl("https://example.com/products/dell-xps-13.jpg")
    .active(true)
    .build();

ProductEntity savedProduct = productRepository.save(laptop);
System.out.println("Producto guardado con ID: " + savedProduct.getId());
```

---

### 2. Buscar Productos

```java
// Búsqueda por SKU
Optional<ProductEntity> product = productRepository.findBySku("LAPTOP-DELL-001");
if (product.isPresent()) {
    System.out.println("Producto encontrado: " + product.get().getName());
}

// Obtener todos los productos activos
List<ProductEntity> activeProducts = productRepository.findByActiveTrue();
System.out.println("Total de productos activos: " + activeProducts.size());

// Productos por categoría con paginación
Page<ProductEntity> electronicPage = productRepository.findByCategoryId(
    "ELECTRONICS", 
    PageRequest.of(0, 10, Sort.by("price").descending())
);
System.out.println("Productos de electrónica: " + electronicPage.getTotalElements());

// Búsqueda full-text
Page<ProductEntity> searchResults = productRepository.searchProducts(
    "laptop",
    PageRequest.of(0, 20)
);
```

---

### 3. Actualizar Inventario

```java
// Simular venta de productos
Optional<ProductEntity> productOpt = productRepository.findById(productId);
if (productOpt.isPresent()) {
    ProductEntity product = productOpt.get();
    
    // Reducir stock
    int quantitySold = 5;
    if (product.getStock() >= quantitySold) {
        product.setStock(product.getStock() - quantitySold);
        product.setUpdatedAt(LocalDateTime.now());
        
        ProductEntity updated = productRepository.save(product);
        System.out.println("Stock actualizado. Nuevo stock: " + updated.getStock());
    } else {
        System.out.println("Inventario insuficiente");
    }
}
```

---

### 4. Encontrar Productos con Bajo Stock

```java
// Obtener productos con menos de 10 unidades
List<ProductEntity> lowStockProducts = productRepository.findLowStockProducts(10);

System.out.println("Productos con bajo stock:");
for (ProductEntity product : lowStockProducts) {
    System.out.printf("%s: %d unidades%n", product.getName(), product.getStock());
}
```

---

### 5. Crear una Orden Completa

```java
@Autowired
private OrderManagementService orderService;
@Autowired
private ProductRepository productRepository;

// Buscar productos disponibles
ProductEntity laptop = productRepository.findBySku("LAPTOP-DELL-001").get();
ProductEntity mouse = productRepository.findBySku("MOUSE-LOGITECH-001").get();

// Preparar items de la orden
List<OrderItemRequest> items = List.of(
    new OrderItemRequest(laptop.getId(), 1),
    new OrderItemRequest(mouse.getId(), 2)
);

// Crear la orden
try {
    OrderEntity order = orderService.createOrder(
        "ORD-20241114-001",
        123L,                    // customerId
        "Juan García López",     // customerName
        "admin",                 // createdBy
        items
    );
    
    System.out.println("Orden creada exitosamente:");
    System.out.println("  Número: " + order.getOrderNumber());
    System.out.println("  Total: " + order.getTotalAmount() + " " + order.getCurrency());
    System.out.println("  Estado: " + order.getStatus());
    System.out.println("  Líneas: " + order.getOrderProducts().size());
    
} catch (IllegalArgumentException e) {
    System.out.println("Error al crear la orden: " + e.getMessage());
}
```

---

### 6. Obtener Orden con Detalles

```java
@Autowired
private OrderRepository orderRepository;

UUID orderId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

// Obtener orden completa con todas sus líneas
Optional<OrderEntity> orderOpt = orderRepository.findByIdWithDetails(orderId);

if (orderOpt.isPresent()) {
    OrderEntity order = orderOpt.get();
    
    System.out.println("=== DETALLES DE LA ORDEN ===");
    System.out.println("Número: " + order.getOrderNumber());
    System.out.println("Cliente: " + order.getCustomerName());
    System.out.println("Estado: " + order.getStatus());
    System.out.println("Total: " + order.getTotalAmount() + " " + order.getCurrency());
    
    System.out.println("\n=== LÍNEAS DE PRODUCTO ===");
    for (OrderProductEntity lineItem : order.getOrderProducts()) {
        System.out.printf("- %s: %d x %s = %s%n",
            lineItem.getProductName(),
            lineItem.getQuantity(),
            lineItem.getUnitPrice(),
            lineItem.getSubtotal()
        );
    }
}
```

---

### 7. Cambiar Estado de Orden

```java
// Transición de estado: PENDING → PROCESSING
try {
    OrderEntity updatedOrder = orderService.updateOrderStatus(
        orderId,
        "PROCESSING"
    );
    System.out.println("Orden actualizada a estado: " + updatedOrder.getStatus());
    
} catch (IllegalArgumentException e) {
    System.out.println("Transición de estado inválida: " + e.getMessage());
}

// Transiciones válidas:
// PENDING → PROCESSING o CANCELLED
// PROCESSING → SHIPPED o CANCELLED
// SHIPPED → DELIVERED
// DELIVERED → (no puede cambiar)
// CANCELLED → (no puede cambiar)
```

---

### 8. Agregar Producto a Orden Existente

```java
try {
    ProductEntity newProduct = productRepository.findBySku("KEYBOARD-MECANIC-001").get();
    
    orderService.addProductToOrder(
        orderId,
        newProduct.getId(),
        1
    );
    
    System.out.println("Producto agregado a la orden");
    
} catch (IllegalStateException e) {
    System.out.println("No se puede agregar a una orden no-PENDING: " + e.getMessage());
} catch (IllegalArgumentException e) {
    System.out.println("Error: " + e.getMessage());
}
```

---

### 9. Remover Producto de Orden

```java
try {
    orderService.removeProductFromOrder(orderId, productId);
    System.out.println("Producto removido de la orden");
    
} catch (IllegalStateException e) {
    System.out.println("No se puede remover de una orden no-PENDING");
} catch (IllegalArgumentException e) {
    System.out.println("Línea de orden no encontrada");
}
```

---

### 10. Consultas Avanzadas

```java
// Órdenes de un cliente específico
Long customerId = 123L;
Page<OrderEntity> customerOrders = orderRepository.findByCustomerId(
    customerId,
    PageRequest.of(0, 10, Sort.by("orderDate").descending())
);
System.out.println("Total órdenes del cliente: " + customerOrders.getTotalElements());

// Órdenes en estado específico
List<OrderEntity> pendingOrders = orderRepository.findByStatus("PENDING");
System.out.println("Órdenes pendientes: " + pendingOrders.size());

// Órdenes en rango de fechas
LocalDateTime startDate = LocalDateTime.of(2024, 11, 1, 0, 0);
LocalDateTime endDate = LocalDateTime.of(2024, 11, 30, 23, 59);

Page<OrderEntity> monthlyOrders = orderRepository.findOrdersByDateRange(
    startDate,
    endDate,
    PageRequest.of(0, 50)
);

// Ingresos del mes
BigDecimal monthlyRevenue = orderRepository.calculateTotalRevenue(startDate);
System.out.println("Ingresos del mes: " + monthlyRevenue);

// Órdenes por cliente y estado
Page<OrderEntity> deliveredCustomerOrders = orderRepository.findByCustomerIdAndStatus(
    customerId,
    "DELIVERED",
    PageRequest.of(0, 20)
);

// Órdenes creadas por usuario específico
Page<OrderEntity> adminOrders = orderRepository.findOrdersByCreator(
    "admin",
    PageRequest.of(0, 10)
);
```

---

### 11. Análisis de Ventas

```java
// Total de productos vendidos
Integer totalQuantitySold = orderProductRepository
    .getTotalQuantitySoldForProduct(productId);
System.out.println("Total vendido de este producto: " + totalQuantitySold);

// Cuántas órdenes contienen este producto
long ordersCount = orderProductRepository
    .countOrdersContainingProduct(productId);
System.out.println("Este producto aparece en " + ordersCount + " órdenes");

// Líneas de una orden específica
List<OrderProductEntity> orderLines = orderProductRepository
    .findLineItemsByOrder(orderId);
System.out.println("Líneas en la orden: " + orderLines.size());

// Cantidad total de órdenes
long totalOrders = orderRepository.count();
System.out.println("Total de órdenes en el sistema: " + totalOrders);

// Órdenes por estado
long pendingCount = orderRepository.countByStatus("PENDING");
long processingCount = orderRepository.countByStatus("PROCESSING");
long deliveredCount = orderRepository.countByStatus("DELIVERED");

System.out.println("Órdenes pendientes: " + pendingCount);
System.out.println("Órdenes procesando: " + processingCount);
System.out.println("Órdenes entregadas: " + deliveredCount);
```

---

### 12. Operaciones en Transacción

```java
@Transactional
public void complexOrderProcess(UUID orderId, String newStatus) {
    // Obtener orden
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
    
    // Actualizar estado
    order.setStatus(newStatus);
    
    // Aplicar descuento si está completa
    if ("PROCESSING".equals(newStatus)) {
        order.setTotalAmount(
            order.getTotalAmount()
                .multiply(new BigDecimal("0.95")) // 5% descuento
        );
    }
    
    // Guardar cambios
    orderRepository.save(order);
    
    // Si algo falla, toda la transacción se revierte automáticamente
}
```

---

### 13. Evitar N+1 Queries

```java
// ❌ MALO: Causa N+1 queries
List<OrderEntity> allOrders = orderRepository.findAll();
for (OrderEntity order : allOrders) {
    // Cada iteración genera una query para orderProducts
    order.getOrderProducts().forEach(op -> 
        System.out.println(op.getProductName())
    );
}

// ✅ BUENO: Una sola query con JOIN FETCH
OrderEntity order = orderRepository.findByIdWithDetails(orderId).get();
order.getOrderProducts().forEach(op ->
    System.out.println(op.getProductName())
);

// ✅ TAMBIÉN BUENO: Usar Specifications o Querydsl
@Query("""
    SELECT o FROM OrderEntity o
    JOIN FETCH o.orderProducts op
    WHERE o.status = 'DELIVERED'
""")
List<OrderEntity> findDeliveredOrdersWithProducts();
```

---

### 14. Paginación y Ordenamiento

```java
import org.springframework.data.domain.*;

// Página 0, 10 items por página, ordenado por precio descendente
Pageable pageable = PageRequest.of(0, 10, Sort.by("price").descending());
Page<ProductEntity> page = productRepository.findByActiveTrue(pageable);

System.out.println("Total elementos: " + page.getTotalElements());
System.out.println("Total páginas: " + page.getTotalPages());
System.out.println("Elemento actual: " + page.getNumber());
System.out.println("Es primera página: " + page.isFirst());
System.out.println("Es última página: " + page.isLast());

// Iteración sobre página
for (ProductEntity product : page) {
    System.out.println(product.getName());
}

// Ir a siguiente página
if (page.hasNext()) {
    Page<ProductEntity> nextPage = productRepository.findByActiveTrue(
        page.nextPageable()
    );
}
```

---

### 15. Manejo de Excepciones

```java
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import jakarta.persistence.EntityNotFoundException;

try {
    // Operación que puede fallar
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
    
    orderService.updateOrderStatus(orderId, "PROCESSING");
    
} catch (OptimisticLockException e) {
    System.out.println("La orden fue modificada por otro usuario. Intente de nuevo.");
    
} catch (EntityNotFoundException e) {
    System.out.println("Recurso no encontrado: " + e.getMessage());
    
} catch (DataIntegrityViolationException e) {
    System.out.println("Violación de integridad de datos");
    
} catch (Exception e) {
    System.out.println("Error inesperado: " + e.getMessage());
}
```

---

## 📊 Patrones de Consulta Comunes

### Pattern: Repository Query Methods

```java
// Nombrado explícitamente
findBy[Property]                    // findByStatus
findBy[Property]AndOtherProperty    // findByStatusAndCustomerId
findBy[Property]OrOtherProperty     // findByStatusOrTotal

// Con Comparadores
findBy[Property]GreaterThan         // findByPriceGreaterThan
findBy[Property]LessThan            // findByStockLessThan
findBy[Property]Between             // findByPriceBetween
findBy[Property]IsNull              // findByImageUrlIsNull
findBy[Property]IsNotNull           // findByCategoryIdIsNotNull

// Con ordenamiento
findBy[Property]OrderBy[AnotherProperty]Asc
findBy[Property]OrderBy[AnotherProperty]Desc
```

---

### Pattern: @Query con JPQL

```java
@Query("""
    SELECT o FROM OrderEntity o
    WHERE o.customerId = :customerId
    AND o.status = :status
    ORDER BY o.orderDate DESC
""")
Page<OrderEntity> findCustomerOrdersByStatus(
    @Param("customerId") Long customerId,
    @Param("status") String status,
    Pageable pageable
);
```

---

### Pattern: Especificaciones

```java
@Transactional(readOnly = true)
public List<OrderEntity> findOrdersWithFilters(
        Long customerId,
        String status,
        LocalDateTime fromDate) {
    
    Specification<OrderEntity> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        if (customerId != null) {
            predicates.add(cb.equal(root.get("customerId"), customerId));
        }
        
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    return orderRepository.findAll(spec);
}
```

---

## 🎯 Tips y Mejores Prácticas

1. **Siempre usar @Transactional**: Garantiza consistencia de transacciones
2. **Evitar N+1 queries**: Usar EntityGraph o JOIN FETCH
3. **Usar DTOs**: No exponer entidades JPA directamente
4. **Validar entrada**: Usar Jakarta Validation
5. **Implementar paginación**: Para grandes conjuntos de datos
6. **Usar índices**: Para columnas frecuentemente consultadas
7. **Configurar lazy loading**: Por defecto lazy, cargar cuando sea necesario
8. **Versionar datos**: Usar @Version para optimistic locking
9. **Auditar cambios**: Usar createdAt/updatedAt automáticamente
10. **Manejar excepciones**: Capturar OptimisticLockException y similares
