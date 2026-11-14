# Diagrama de Relaciones - Entidades JPA

## 📊 Modelo Entidad-Relación (ER)

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                         SCHEMA: public                                 │
│                                                                        │
│   ┌─────────────────────────┐         ┌──────────────────────────┐   │
│   │    PRODUCTS TABLE       │         │    ORDERS TABLE          │   │
│   ├─────────────────────────┤         ├──────────────────────────┤   │
│   │ id (UUID, PK)◄──────┐   │         │ id (UUID, PK)        │   │   │
│   │ sku (VARCHAR, UNQ)  │   │         │ order_number (UNQ)   │   │   │
│   │ name (VARCHAR)      │   │         │ customer_id (BIGINT) │   │   │
│   │ description (TEXT)  │   │         │ customer_name (VC)   │   │   │
│   │ price (NUMERIC)     │   │         │ created_by (VC)      │   │   │
│   │ stock (INT)         │   │         │ order_date (TS)      │   │   │
│   │ category_id (VC)    │   │         │ status (VC)          │   │   │
│   │ image_url (VC)      │   │         │ total_amount (NUM)   │   │   │
│   │ active (BOOL)       │   │         │ currency (VC)        │   │   │
│   │ created_at (TS)     │   │         │ created_at (TS)      │   │   │
│   │ updated_at (TS)     │   │         │ updated_at (TS)      │   │   │
│   │ version (BIGINT)    │   │         │ version (BIGINT)     │   │   │
│   └─────────────────────────┘         └──────────────────────────┘   │
│                 ▲                                   ▲                  │
│                 │                                   │                  │
│                 │        ┌──────────────────────────┘                  │
│                 │        │    FK: order_id (CASCADE)                  │
│   ┌─────────────┼────────┴────────────────────────┐                  │
│   │             │                                 │                   │
│   │   ┌─────────┴──────────────────────────────────┴────┐             │
│   │   │        ORDER_PRODUCTS TABLE (Composite PK)    │             │
│   │   ├─────────────────────────────────────────────┤             │
│   │   │ id (EmbeddedId):                            │             │
│   │   │   - order_id (UUID, PK, FK) ────────┐      │             │
│   │   │   - product_id (UUID, PK, FK) ──────┼──┐   │             │
│   │   │ product_name (VARCHAR)               │  │   │             │
│   │   │ quantity (INT)                       │  │   │             │
│   │   │ unit_price (NUMERIC)                 │  │   │             │
│   │   │ subtotal (NUMERIC, COMPUTED)         │  │   │             │
│   │   │ created_at (TS)                      │  │   │             │
│   │   │ updated_at (TS)                      │  │   │             │
│   │   │ version (BIGINT)                     │  │   │             │
│   │   └────────────────────────────────────────┼──┼──┘             │
│   │                                            │  │                 │
│   └────────────────────────────────────────────┘  │                 │
│                                                    │                 │
│                                                    └─────────────────┘
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🔗 Relaciones de Entidades

### 1. ProductEntity ↔ OrderProductEntity
- **Tipo:** One-to-Many (1:N)
- **Dirección:** Bidireccional
- **Lado Propietario:** OrderProductEntity
- **Mapeo:**
  ```java
  // En ProductEntity
  @OneToMany(mappedBy = "product", cascade = CascadeType.REFRESH, ...)
  private Set<OrderProductEntity> orderProducts;

  // En OrderProductEntity
  @ManyToOne(fetch = FetchType.LAZY, ...)
  @JoinColumn(name = "product_id", ...)
  private ProductEntity product;
  ```
- **Acciones:**
  - ON DELETE: RESTRICT (No permite eliminar un producto que está en órdenes)
  - ON UPDATE: NO ACTION

---

### 2. OrderEntity ↔ OrderProductEntity
- **Tipo:** One-to-Many (1:N)
- **Dirección:** Bidireccional
- **Lado Propietario:** OrderProductEntity
- **Mapeo:**
  ```java
  // En OrderEntity
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, ...)
  private Set<OrderProductEntity> orderProducts;

  // En OrderProductEntity
  @ManyToOne(fetch = FetchType.LAZY, ...)
  @JoinColumn(name = "order_id", ...)
  private OrderEntity order;
  ```
- **Acciones:**
  - ON DELETE: CASCADE (Al eliminar una orden, se eliminan todas sus líneas)
  - ON UPDATE: NO ACTION
  - orphanRemoval = true: Los OrderProducts huérfanos se eliminan automáticamente

---

### 3. OrderProductEntity - Clave Compuesta
- **Tipo:** EmbeddedId
- **Clase:** `OrderProductId.java`
- **Atributos:**
  - orderId: UUID
  - productId: UUID
- **Restricción:** UNIQUE(order_id, product_id)

---

## 📈 Cardinalities

```
PRODUCTS              ORDER_PRODUCTS           ORDERS
┌──────────┐  1    M ┌──────────────┐  1    M ┌────────┐
│ Product  │◄────────│ OrderProduct  │────────►│ Order  │
└──────────┘         └──────────────┘         └────────┘
   (1:N)                (Pivote)                 (1:N)
```

---

## 🔄 Ciclo de Vida - Cascadas

### OrderEntity (Propietaria de la relación)

```
CREATE ORDER
    ├─ CascadeType.ALL
    ├─ FetchType.LAZY
    └─ orphanRemoval = true
        ├─ PERSIST → OrderProducts se crean
        ├─ MERGE → OrderProducts se actualizan
        ├─ REMOVE → OrderProducts se eliminan
        ├─ REFRESH → OrderProducts se refrescan
        ├─ DETACH → OrderProducts se desvinculan
        └─ Removing an OrderProduct → Se elimina de la BD
```

### ProductEntity (No propietaria)

```
MANAGE PRODUCT
    ├─ CascadeType.REFRESH
    ├─ FetchType.LAZY
    └─ orphanRemoval = false
        ├─ Solo propaga REFRESH
        ├─ No elimina OrderProducts al eliminar Product
        └─ RESTRICT previene eliminación de Products con órdenes
```

---

## 🗝️ Claves Primarias y Foráneas

### Primary Keys (PK)
1. **products.id** - UUID (SERIAL auto)
2. **orders.id** - UUID (SERIAL auto)
3. **order_products.(order_id, product_id)** - Composite (PK)

### Foreign Keys (FK)
1. **order_products.order_id** → **orders.id**
   - Constraint: `fk_order_products_order`
   - ON DELETE: CASCADE
   - ON UPDATE: NO ACTION

2. **order_products.product_id** → **products.id**
   - Constraint: `fk_order_products_product`
   - ON DELETE: RESTRICT
   - ON UPDATE: NO ACTION

---

## 📍 Unique Constraints

```
products
  └─ sku (UNIQUE)

orders
  └─ order_number (UNIQUE)

order_products
  └─ (order_id, product_id) UNIQUE
```

---

## 🏗️ Índices para Optimización

### products
```
CREATE INDEX idx_product_sku ON products(sku) UNIQUE;
CREATE INDEX idx_product_category ON products(category_id);
CREATE INDEX idx_product_active ON products(active);
```

### orders
```
CREATE INDEX idx_order_number ON orders(order_number) UNIQUE;
CREATE INDEX idx_order_customer ON orders(customer_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_date ON orders(order_date);
```

### order_products
```
CREATE INDEX idx_order_product_order ON order_products(order_id);
CREATE INDEX idx_order_product_product ON order_products(product_id);
```

---

## 🔐 Transacciones y Concurrencia

### Optimistic Locking

```
Entidad          Campo     Tipo      Comportamiento
──────────────────────────────────────────────────────
ProductEntity    version   BIGINT    Se incrementa en cada UPDATE
OrderEntity      version   BIGINT    Se incrementa en cada UPDATE
OrderProductEntity version BIGINT    Se incrementa en cada UPDATE
```

**Flujo:**
1. Lectura: SELECT ... version = 1
2. Modificación: UPDATE ... SET version = 2 WHERE version = 1
3. Si versión cambia: OptimisticLockException

---

## ⏰ Auditoría

### Campos Automáticos

```
Entidad          createdAt        updatedAt        Generados Por
─────────────────────────────────────────────────────────────────
ProductEntity    @CreationTS      @UpdateTS        Hibernate
OrderEntity      @CreationTS      @UpdateTS        Hibernate
OrderProductEntity @CreationTS    @UpdateTS        Hibernate
```

**Características:**
- createdAt: Inmutable (NO SE ACTUALIZA)
- updatedAt: Se actualiza en cada cambio
- Generado en el servidor de BD para consistencia

---

## 📋 Estados Válidos de Orden

```
State Diagram:

    ┌──────────┐
    │ PENDING  │◄──────────────┐
    └────┬─────┘               │
         │                     │ CANCELLED
         │                     │
         ▼                     │
    ┌──────────────┐           │
    │ PROCESSING   │───────────┘
    └────┬─────────┘
         │
         ▼
    ┌────────────┐
    │  SHIPPED   │
    └────┬───────┘
         │
         ▼
    ┌──────────────┐
    │ DELIVERED    │
    └──────────────┘
```

---

## 🔍 Consultas Típicas (Query Patterns)

### N+1 Query Problem

❌ **MALO:**
```java
List<Order> orders = orderRepository.findAll();
for (Order o : orders) {
    // Cada iteración ejecuta una query para orderProducts
    Set<OrderProduct> products = o.getOrderProducts();
}
```

✅ **BUENO:**
```java
@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
Optional<OrderEntity> findByIdWithDetails(UUID id);

OrderEntity order = orderRepository.findByIdWithDetails(id).get();
// orderProducts ya está cargado, sin queries adicionales
```

---

## 📊 Ejemplo de Flujo Completo

```
1. Crear Orden
   CREATE ORDER (PK: UUID, status: PENDING, total: 0)

2. Agregar Productos
   INSERT ORDER_PRODUCT (FK: order_id, product_id, qty, price)
   → Subtotal calculado automáticamente
   → Total de orden se actualiza

3. Confirmar Orden
   UPDATE ORDER SET status = PROCESSING, total = sum(subtotals)

4. Embarque
   UPDATE ORDER SET status = SHIPPED

5. Entrega
   UPDATE ORDER SET status = DELIVERED

6. Anular Línea
   DELETE ORDER_PRODUCT WHERE order_id = X AND product_id = Y
   → orphanRemoval elimina registro
   → Inventario se restaura
   → Total se recalcula

7. Eliminar Orden
   DELETE ORDER (cascade = true)
   → Se eliminan automáticamente todos los ORDER_PRODUCTS
```

---

## 🎯 Decisiones de Diseño

### Por qué UUID en lugar de SERIAL
- ✅ Generación distribuida
- ✅ No expone información (IDs no secuenciales)
- ✅ Escalabilidad horizontal

### Por qué Composite Key en OrderProduct
- ✅ Evita duplicados (un producto por orden, una sola vez)
- ✅ Integridad referencial explícita
- ✅ Queries eficientes (ambas FK incluidas en PK)

### Por qué CASCADE para OrderProduct
- ✅ Las líneas no tienen sentido sin la orden
- ✅ Simplifica gestión del ciclo de vida
- ✅ Evita órdenes "huérfanas"

### Por qué RESTRICT para Product
- ✅ No permite eliminar productos vendidos
- ✅ Mantiene histórico de ventas
- ✅ Integridad de datos

### Por qué Optimistic Locking
- ✅ Mejor performance que pessimistic locks
- ✅ Evita deadlocks
- ✅ Apropiado para aplicaciones web

---

## 📚 Referencias

- [JPA Specification - Section 9](https://download.oracle.com/otndocs/jcp/persistence-2_2-fr-eval-spec/index.html)
- [Hibernate Relations Documentation](https://hibernate.org/orm/documentation/latest/userguide/#associations)
- [PostgreSQL Foreign Keys](https://www.postgresql.org/docs/current/sql-createtable.html#SQL-CREATETABLE-REFERENCES)
