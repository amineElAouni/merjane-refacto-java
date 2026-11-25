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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class NormalProductOrderHandlerTests {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NormalProductOrderHandler handler;

    @Test
    void supportsNormalProducts() {
        assertThat(handler.supportedType()).isEqualTo(ProductType.NORMAL);
    }

    @Test
    void decrementsStockWhenInStock() {
        Product usbCable = normalProduct(30, 15);

        handler.handle(usbCable, TODAY);

        assertThat(usbCable.getAvailable()).isEqualTo(29);
        verifyNoInteractions(notificationService);
    }

    @Test
    void notifiesDelayWhenOutOfStock() {
        Product usbCable = normalProduct(0, 15);

        handler.handle(usbCable, TODAY);

        assertThat(usbCable.getAvailable()).isZero();
        verify(notificationService).sendDelayNotification(15, "USB Cable");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void doesNothingWhenOutOfStockWithoutLeadTime() {
        Product usbCable = normalProduct(0, 0);

        handler.handle(usbCable, TODAY);

        assertThat(usbCable.getAvailable()).isZero();
        verifyNoInteractions(notificationService);
    }

    private static Product normalProduct(int available, int leadTime) {
        return new Product(null, leadTime, available, "NORMAL", "USB Cable", null, null, null);
    }
}
