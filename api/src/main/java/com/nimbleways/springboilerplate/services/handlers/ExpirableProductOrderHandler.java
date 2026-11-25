package com.nimbleways.springboilerplate.services.handlers;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class ExpirableProductOrderHandler implements ProductOrderHandler {

    private final NotificationService notificationService;

    public ExpirableProductOrderHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.EXPIRABLE;
    }

    @Override
    public void handle(Product product, LocalDate today) {
        if (product.isInStock() && !product.isExpired(today)) {
            product.decrementStock();
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.emptyStock();
        }
    }
}
