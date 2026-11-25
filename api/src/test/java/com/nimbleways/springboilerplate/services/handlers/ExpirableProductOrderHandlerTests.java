package com.nimbleways.springboilerplate.services.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ExpirableProductOrderHandlerTests {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExpirableProductOrderHandler handler;

    @Test
    void supportsExpirableProducts() {
        assertThat(handler.supportedType()).isEqualTo(ProductType.EXPIRABLE);
    }

    @Test
    void decrementsStockWhenNotExpiredAndInStock() {
        Product milk = expirableProduct(30, TODAY.plusDays(26));

        handler.handle(milk, TODAY);

        assertThat(milk.getAvailable()).isEqualTo(29);
        verifyNoInteractions(notificationService);
    }

    @Test
    void notifiesExpirationAndEmptiesStockWhenExpired() {
        LocalDate expiryDate = TODAY.minusDays(2);
        Product milk = expirableProduct(6, expiryDate);

        handler.handle(milk, TODAY);

        assertThat(milk.getAvailable()).isZero();
        verify(notificationService).sendExpirationNotification("Milk", expiryDate);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void isExpiredOnItsExpiryDate() {
        Product milk = expirableProduct(6, TODAY);

        handler.handle(milk, TODAY);

        assertThat(milk.getAvailable()).isZero();
        verify(notificationService).sendExpirationNotification("Milk", TODAY);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("legacy: out of stock but not expired product triggers an expiration notification")
    void legacyNotifiesExpirationWhenOutOfStockEvenIfNotExpired() {
        LocalDate expiryDate = TODAY.plusDays(26);
        Product milk = expirableProduct(0, expiryDate);

        handler.handle(milk, TODAY);

        assertThat(milk.getAvailable()).isZero();
        verify(notificationService).sendExpirationNotification("Milk", expiryDate);
        verifyNoMoreInteractions(notificationService);
    }

    private static Product expirableProduct(int available, LocalDate expiryDate) {
        return new Product(null, 15, available, "EXPIRABLE", "Milk", expiryDate, null, null);
    }
}
