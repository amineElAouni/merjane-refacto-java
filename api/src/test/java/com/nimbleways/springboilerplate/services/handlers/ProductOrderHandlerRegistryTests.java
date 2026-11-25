package com.nimbleways.springboilerplate.services.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.util.List;

import org.junit.jupiter.api.Test;

@UnitTest
class ProductOrderHandlerRegistryTests {

    private final NotificationService notificationService = mock(NotificationService.class);

    private final ProductOrderHandlerRegistry registry = new ProductOrderHandlerRegistry(List.of(
            new NormalProductOrderHandler(notificationService),
            new SeasonalProductOrderHandler(notificationService),
            new ExpirableProductOrderHandler(notificationService)));

    @Test
    void findsTheHandlerMatchingTheProductType() {
        assertThat(registry.handlerFor(productOfType("NORMAL"))).containsInstanceOf(NormalProductOrderHandler.class);
        assertThat(registry.handlerFor(productOfType("SEASONAL"))).containsInstanceOf(SeasonalProductOrderHandler.class);
        assertThat(registry.handlerFor(productOfType("EXPIRABLE"))).containsInstanceOf(ExpirableProductOrderHandler.class);
    }

    @Test
    void findsNoHandlerForAnUnknownProductType() {
        assertThat(registry.handlerFor(productOfType("FLASHSALE"))).isEmpty();
    }

    private static Product productOfType(String type) {
        return new Product(null, 15, 30, type, "Any product", null, null, null);
    }
}
