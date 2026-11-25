package com.nimbleways.springboilerplate.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /orders/{orderId}/processOrder")
class ProcessOrderIntegrationTests {

    private static final LocalDate TODAY = LocalDate.now();
    private static final long UNKNOWN_ORDER_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Nested
    @DisplayName("NORMAL product")
    class NormalProduct {

        @Test
        void decrementsStockWhenInStock() throws Exception {
            Product usbCable = normalProduct("USB Cable", 30, 15);

            Product processed = processOrderOf(usbCable);

            assertThat(processed.getAvailable()).isEqualTo(29);
            verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesDelayWhenOutOfStock() throws Exception {
            Product usbDongle = normalProduct("USB Dongle", 0, 10);

            Product processed = processOrderOf(usbDongle);

            assertThat(processed.getAvailable()).isZero();
            assertThat(processed.getLeadTime()).isEqualTo(10);
            verify(notificationService).sendDelayNotification(10, "USB Dongle");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void doesNothingWhenOutOfStockWithoutLeadTime() throws Exception {
            Product usbHub = normalProduct("USB Hub", 0, 0);

            Product processed = processOrderOf(usbHub);

            assertThat(processed.getAvailable()).isZero();
            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("SEASONAL product")
    class SeasonalProduct {

        @Test
        void decrementsStockWhenInSeasonAndInStock() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 30, 15, TODAY.minusDays(2), TODAY.plusDays(58));

            Product processed = processOrderOf(watermelon);

            assertThat(processed.getAvailable()).isEqualTo(29);
            verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesDelayWhenOutOfStockAndRestockedBeforeSeasonEnd() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 0, 15, TODAY.minusDays(2), TODAY.plusDays(58));

            Product processed = processOrderOf(watermelon);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendDelayNotification(15, "Watermelon");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesDelayWhenRestockedOnTheLastDayOfSeason() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 0, 15, TODAY.minusDays(2), TODAY.plusDays(15));

            processOrderOf(watermelon);

            verify(notificationService).sendDelayNotification(15, "Watermelon");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesUnavailabilityWhenOutOfStockAndRestockedAfterSeasonEnd() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 0, 90, TODAY.minusDays(2), TODAY.plusDays(58));

            Product processed = processOrderOf(watermelon);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendOutOfStockNotification("Watermelon");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesUnavailabilityAndKeepsStockWhenSeasonHasNotStarted() throws Exception {
            Product grapes = seasonalProduct("Grapes", 30, 15, TODAY.plusDays(180), TODAY.plusDays(240));

            Product processed = processOrderOf(grapes);

            assertThat(processed.getAvailable()).isEqualTo(30);
            verify(notificationService).sendOutOfStockNotification("Grapes");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesUnavailabilityAndEmptiesStockWhenSeasonIsOver() throws Exception {
            Product grapes = seasonalProduct("Grapes", 30, 15, TODAY.minusDays(60), TODAY.minusDays(10));

            Product processed = processOrderOf(grapes);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendOutOfStockNotification("Grapes");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: empties stock when season has not started and restock would land after season end")
        void legacyEmptiesStockWhenSeasonNotStartedAndRestockedAfterSeasonEnd() throws Exception {
            Product grapes = seasonalProduct("Grapes", 30, 30, TODAY.plusDays(10), TODAY.plusDays(20));

            Product processed = processOrderOf(grapes);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendOutOfStockNotification("Grapes");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: first day of season is not in season, in-stock product is delayed instead of sold")
        void legacyNotifiesDelayOnFirstDayOfSeasonEvenWhenInStock() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 30, 15, TODAY, TODAY.plusDays(58));

            Product processed = processOrderOf(watermelon);

            assertThat(processed.getAvailable()).isEqualTo(30);
            verify(notificationService).sendDelayNotification(15, "Watermelon");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: last day of season is not in season, in-stock product becomes unavailable")
        void legacyNotifiesUnavailabilityOnLastDayOfSeasonEvenWhenInStock() throws Exception {
            Product watermelon = seasonalProduct("Watermelon", 30, 15, TODAY.minusDays(58), TODAY);

            Product processed = processOrderOf(watermelon);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendOutOfStockNotification("Watermelon");
            verifyNoMoreInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("EXPIRABLE product")
    class ExpirableProduct {

        @Test
        void decrementsStockWhenNotExpiredAndInStock() throws Exception {
            Product butter = expirableProduct("Butter", 30, TODAY.plusDays(26));

            Product processed = processOrderOf(butter);

            assertThat(processed.getAvailable()).isEqualTo(29);
            verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesExpirationAndEmptiesStockWhenExpired() throws Exception {
            LocalDate expiryDate = TODAY.minusDays(2);
            Product milk = expirableProduct("Milk", 6, expiryDate);

            Product processed = processOrderOf(milk);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendExpirationNotification("Milk", expiryDate);
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        void isExpiredOnItsExpiryDate() throws Exception {
            Product milk = expirableProduct("Milk", 6, TODAY);

            Product processed = processOrderOf(milk);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendExpirationNotification("Milk", TODAY);
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: out of stock but not expired product triggers an expiration notification")
        void legacyNotifiesExpirationWhenOutOfStockEvenIfNotExpired() throws Exception {
            LocalDate expiryDate = TODAY.plusDays(26);
            Product butter = expirableProduct("Butter", 0, expiryDate);

            Product processed = processOrderOf(butter);

            assertThat(processed.getAvailable()).isZero();
            verify(notificationService).sendExpirationNotification("Butter", expiryDate);
            verifyNoMoreInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("order")
    class WholeOrder {

        @Test
        void returnsTheOrderIdAndProcessesEveryItem() throws Exception {
            Product usbCable = normalProduct("USB Cable", 30, 15);
            Product usbDongle = normalProduct("USB Dongle", 0, 10);
            Product milk = expirableProduct("Milk", 6, TODAY.minusDays(2));
            Product grapes = seasonalProduct("Grapes", 30, 15, TODAY.plusDays(180), TODAY.plusDays(240));
            Order order = saveOrderOf(usbCable, usbDongle, milk, grapes);

            mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId().intValue()));

            assertThat(reload(usbCable).getAvailable()).isEqualTo(29);
            assertThat(reload(milk).getAvailable()).isZero();
            verify(notificationService).sendDelayNotification(10, "USB Dongle");
            verify(notificationService).sendExpirationNotification("Milk", TODAY.minusDays(2));
            verify(notificationService).sendOutOfStockNotification("Grapes");
            verifyNoMoreInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: product with an unknown type is ignored")
        void legacyIgnoresProductOfUnknownType() throws Exception {
            Product flashSale = new Product(null, 15, 30, "FLASHSALE", "Headphones", null, null, null);

            Product processed = processOrderOf(flashSale);

            assertThat(processed.getAvailable()).isEqualTo(30);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("legacy: unknown order is not handled and fails with NoSuchElementException")
        void legacyFailsWhenOrderDoesNotExist() {
            assertThatThrownBy(() -> mockMvc.perform(post("/orders/{orderId}/processOrder", UNKNOWN_ORDER_ID)))
                    .hasCauseInstanceOf(NoSuchElementException.class);
        }
    }

    private Product processOrderOf(Product product) throws Exception {
        Order order = saveOrderOf(product);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        return reload(product);
    }

    private Order saveOrderOf(Product... products) {
        Set<Product> items = new LinkedHashSet<>(productRepository.saveAll(List.of(products)));
        Order order = new Order();
        order.setItems(items);
        return orderRepository.save(order);
    }

    private Product reload(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }

    private static Product normalProduct(String name, int available, int leadTime) {
        return new Product(null, leadTime, available, "NORMAL", name, null, null, null);
    }

    private static Product seasonalProduct(String name, int available, int leadTime,
            LocalDate seasonStartDate, LocalDate seasonEndDate) {
        return new Product(null, leadTime, available, "SEASONAL", name, null, seasonStartDate, seasonEndDate);
    }

    private static Product expirableProduct(String name, int available, LocalDate expiryDate) {
        return new Product(null, 15, available, "EXPIRABLE", name, expiryDate, null, null);
    }
}
