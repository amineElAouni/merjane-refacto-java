package com.nimbleways.springboilerplate.services.handlers;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class SeasonalProductOrderHandler implements ProductOrderHandler {

    private final NotificationService notificationService;

    public SeasonalProductOrderHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.SEASONAL;
    }

    @Override
    public void handle(Product product, LocalDate today) {
        if (product.isInSeason(today) && product.isInStock()) {
            product.decrementStock();
        } else if (product.isRestockedAfterSeasonEnd(today)) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.emptyStock();
        } else if (product.isBeforeSeason(today)) {
            notificationService.sendOutOfStockNotification(product.getName());
        } else {
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        }
    }
}
