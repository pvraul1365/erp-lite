package net.javaguides.erp_lite.infrastructure.service;

import lombok.RequiredArgsConstructor;
import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderEntity;
import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderProductEntity;
import net.javaguides.erp_lite.infrastructure.persistence.entity.OrderProductId;
import net.javaguides.erp_lite.infrastructure.persistence.entity.ProductEntity;
import net.javaguides.erp_lite.infrastructure.persistence.repository.OrderProductRepository;
import net.javaguides.erp_lite.infrastructure.persistence.repository.OrderRepository;
import net.javaguides.erp_lite.infrastructure.persistence.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de órdenes.
 * 
 * Proporciona operaciones de alto nivel para crear, actualizar y consultar órdenes,
 * junto con la gestión de sus líneas de producto.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;

    /**
     * Crea una nueva orden con líneas de producto.
     *
     * @param orderNumber Número único de la orden
     * @param customerId ID del cliente
     * @param customerName Nombre del cliente
     * @param createdBy Usuario que crea la orden
     * @param items Lista de items a incluir (productId, quantity)
     * @return Orden creada
     * @throws IllegalArgumentException si no hay suficiente inventario
     */
    public OrderEntity createOrder(
            String orderNumber,
            Long customerId,
            String customerName,
            String createdBy,
            List<OrderItemRequest> items) {

        // Validar que no exista una orden con el mismo número
        if (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            throw new IllegalArgumentException("Número de orden ya existe: " + orderNumber);
        }

        // Crear la orden base
        OrderEntity order = OrderEntity.builder()
                .orderNumber(orderNumber)
                .customerId(customerId)
                .customerName(customerName)
                .createdBy(createdBy)
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .currency("USD")
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Procesar cada item de la orden
        for (OrderItemRequest item : items) {
            ProductEntity product = productRepository.findById(item.productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + item.productId));

            // Validar inventario
            if (product.getStock() < item.quantity) {
                throw new IllegalArgumentException(
                        "Inventario insuficiente para: " + product.getName() +
                        ". Disponible: " + product.getStock() +
                        ", Solicitado: " + item.quantity);
            }

            // Crear línea de orden
            OrderProductEntity lineItem = OrderProductEntity.builder()
                    .id(new OrderProductId(order.getId(), product.getId()))
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .quantity(item.quantity)
                    .unitPrice(product.getPrice())
                    .build();

            order.addOrderProduct(lineItem);
            totalAmount = totalAmount.add(lineItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        // Persistir orden y sus líneas
        OrderEntity savedOrder = orderRepository.save(order);

        // Actualizar inventario
        for (OrderItemRequest item : items) {
            ProductEntity product = productRepository.findById(item.productId).get();
            product.setStock(product.getStock() - item.quantity);
            productRepository.save(product);
        }

        return savedOrder;
    }

    /**
     * Agrega un producto a una orden existente.
     *
     * @param orderId ID de la orden
     * @param productId ID del producto
     * @param quantity Cantidad
     * @throws IllegalStateException si la orden no está en estado PENDING
     */
    public void addProductToOrder(UUID orderId, UUID productId, Integer quantity) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Solo se pueden agregar productos a órdenes PENDING");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Inventario insuficiente para: " + product.getName());
        }

        // Crear nueva línea
        OrderProductEntity lineItem = OrderProductEntity.builder()
                .id(new OrderProductId(orderId, productId))
                .order(order)
                .product(product)
                .productName(product.getName())
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();

        order.addOrderProduct(lineItem);

        // Actualizar total
        BigDecimal newTotal = order.getTotalAmount().add(lineItem.getSubtotal());
        order.setTotalAmount(newTotal);

        // Actualizar inventario
        product.setStock(product.getStock() - quantity);

        orderRepository.save(order);
        productRepository.save(product);
    }

    /**
     * Cancela una línea de producto de una orden.
     *
     * @param orderId ID de la orden
     * @param productId ID del producto a remover
     * @throws IllegalStateException si la orden no está en estado PENDING
     */
    public void removeProductFromOrder(UUID orderId, UUID productId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Solo se pueden remover productos de órdenes PENDING");
        }

        OrderProductId id = new OrderProductId(orderId, productId);
        OrderProductEntity lineItem = orderProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Línea de orden no encontrada"));

        // Devolver inventario
        ProductEntity product = lineItem.getProduct();
        product.setStock(product.getStock() + lineItem.getQuantity());

        // Actualizar total de la orden
        BigDecimal newTotal = order.getTotalAmount().subtract(lineItem.getSubtotal());
        order.setTotalAmount(newTotal);

        order.removeOrderProduct(lineItem);

        orderRepository.save(order);
        productRepository.save(product);
    }

    /**
     * Cambia el estado de una orden.
     *
     * @param orderId ID de la orden
     * @param newStatus Nuevo estado
     * @return Orden actualizada
     */
    public OrderEntity updateOrderStatus(UUID orderId, String newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + orderId));

        // Validar transiciones de estado (ejemplo simple)
        String currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                    "Transición de estado inválida: " + currentStatus + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /**
     * Obtiene una orden con todos sus detalles (incluye lazy-loaded relations).
     *
     * @param orderId ID de la orden
     * @return Orden con detalles
     */
    @Transactional(readOnly = true)
    public OrderEntity getOrderWithDetails(UUID orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + orderId));
    }

    /**
     * Calcula el total de un pedido basado en sus líneas.
     *
     * @param orderId ID de la orden
     * @return Total calculado
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOrderTotal(UUID orderId) {
        List<OrderProductEntity> items = orderProductRepository.findByOrderId(orderId);
        return items.stream()
                .map(OrderProductEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valida si una transición de estado es permitida.
     */
    private boolean isValidStatusTransition(String from, String to) {
        return switch (from) {
            case "PENDING" -> to.equals("PROCESSING") || to.equals("CANCELLED");
            case "PROCESSING" -> to.equals("SHIPPED") || to.equals("CANCELLED");
            case "SHIPPED" -> to.equals("DELIVERED");
            case "DELIVERED", "CANCELLED" -> false;
            default -> false;
        };
    }
}
