package com.nimbleways.springboilerplate.services.handlers;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class NormalProductOrderHandler implements ProductOrderHandler {

    private final NotificationService notificationService;

    public NormalProductOrderHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.NORMAL;
    }

    @Override
    public void handle(Product product, LocalDate today) {
        if (product.isInStock()) {
            product.decrementStock();
        } else if (product.hasLeadTime()) {
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        }
    }
}
