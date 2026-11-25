package com.nimbleways.springboilerplate.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.handlers.ExpirableProductOrderHandler;
import com.nimbleways.springboilerplate.services.handlers.NormalProductOrderHandler;
import com.nimbleways.springboilerplate.services.handlers.ProductOrderHandlerRegistry;
import com.nimbleways.springboilerplate.services.handlers.SeasonalProductOrderHandler;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final long ORDER_ID = 42L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ProductOrderHandlerRegistry handlers = new ProductOrderHandlerRegistry(List.of(
                new NormalProductOrderHandler(notificationService),
                new SeasonalProductOrderHandler(notificationService),
                new ExpirableProductOrderHandler(notificationService)));
        orderService = new OrderService(orderRepository, productRepository, handlers, CLOCK);
    }

    @Test
    void processesAndSavesEveryProductOfTheOrder() {
        Product usbCable = new Product(1L, 15, 30, "NORMAL", "USB Cable", null, null, null);
        Product milk = new Product(2L, 15, 6, "EXPIRABLE", "Milk", TODAY.minusDays(2), null, null);
        Order order = orderOf(usbCable, milk);

        Order processedOrder = orderService.processOrder(ORDER_ID);

        assertThat(processedOrder).isSameAs(order);
        assertThat(usbCable.getAvailable()).isEqualTo(29);
        assertThat(milk.getAvailable()).isZero();
        verify(productRepository).save(usbCable);
        verify(productRepository).save(milk);
        verify(notificationService).sendExpirationNotification("Milk", TODAY.minusDays(2));
    }

    @Test
    void usesTheClockToEvaluateDateRules() {
        Product expiresTomorrow = new Product(1L, 15, 6, "EXPIRABLE", "Milk", TODAY.plusDays(1), null, null);
        orderOf(expiresTomorrow);

        orderService.processOrder(ORDER_ID);

        assertThat(expiresTomorrow.getAvailable()).isEqualTo(5);
        verifyNoInteractions(notificationService);
    }

    @Test
    void ignoresProductsOfUnknownType() {
        Product flashSale = new Product(1L, 15, 30, "FLASHSALE", "Headphones", null, null, null);
        orderOf(flashSale);

        orderService.processOrder(ORDER_ID);

        assertThat(flashSale.getAvailable()).isEqualTo(30);
        verify(productRepository, never()).save(flashSale);
        verifyNoInteractions(notificationService);
    }

    @Test
    void failsWhenTheOrderDoesNotExist() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.processOrder(ORDER_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Order orderOf(Product... products) {
        Order order = new Order(ORDER_ID, new LinkedHashSet<>(List.of(products)));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        return order;
    }
}
