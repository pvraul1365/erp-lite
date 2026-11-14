package net.javaguides.erp_lite.infrastructure.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO para solicitud de item de orden
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    public UUID productId;
    public Integer quantity;
}
