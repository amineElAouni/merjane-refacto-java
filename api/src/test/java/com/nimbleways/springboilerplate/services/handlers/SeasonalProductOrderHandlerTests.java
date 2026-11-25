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
class SeasonalProductOrderHandlerTests {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);
    private static final LocalDate SEASON_START = TODAY.minusDays(2);
    private static final LocalDate SEASON_END = TODAY.plusDays(58);

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SeasonalProductOrderHandler handler;

    @Test
    void supportsSeasonalProducts() {
        assertThat(handler.supportedType()).isEqualTo(ProductType.SEASONAL);
    }

    @Test
    void decrementsStockWhenInSeasonAndInStock() {
        Product watermelon = seasonalProduct(30, 15, SEASON_START, SEASON_END);

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isEqualTo(29);
        verifyNoInteractions(notificationService);
    }

    @Test
    void notifiesDelayWhenOutOfStockAndRestockedBeforeSeasonEnd() {
        Product watermelon = seasonalProduct(0, 15, SEASON_START, SEASON_END);

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isZero();
        verify(notificationService).sendDelayNotification(15, "Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void notifiesDelayWhenRestockedOnTheLastDayOfSeason() {
        Product watermelon = seasonalProduct(0, 15, SEASON_START, TODAY.plusDays(15));

        handler.handle(watermelon, TODAY);

        verify(notificationService).sendDelayNotification(15, "Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void notifiesUnavailabilityWhenOutOfStockAndRestockedAfterSeasonEnd() {
        Product watermelon = seasonalProduct(0, 90, SEASON_START, SEASON_END);

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isZero();
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void notifiesUnavailabilityAndKeepsStockWhenSeasonHasNotStarted() {
        Product watermelon = seasonalProduct(30, 15, TODAY.plusDays(180), TODAY.plusDays(240));

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isEqualTo(30);
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void notifiesUnavailabilityAndEmptiesStockWhenSeasonIsOver() {
        Product watermelon = seasonalProduct(30, 15, TODAY.minusDays(60), TODAY.minusDays(10));

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isZero();
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("legacy: empties stock when season has not started and restock would land after season end")
    void legacyEmptiesStockWhenSeasonNotStartedAndRestockedAfterSeasonEnd() {
        Product watermelon = seasonalProduct(30, 30, TODAY.plusDays(10), TODAY.plusDays(20));

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isZero();
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("legacy: first day of season is not in season, in-stock product is delayed instead of sold")
    void legacyNotifiesDelayOnFirstDayOfSeasonEvenWhenInStock() {
        Product watermelon = seasonalProduct(30, 15, TODAY, SEASON_END);

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isEqualTo(30);
        verify(notificationService).sendDelayNotification(15, "Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("legacy: last day of season is not in season, in-stock product becomes unavailable")
    void legacyNotifiesUnavailabilityOnLastDayOfSeasonEvenWhenInStock() {
        Product watermelon = seasonalProduct(30, 15, TODAY.minusDays(58), TODAY);

        handler.handle(watermelon, TODAY);

        assertThat(watermelon.getAvailable()).isZero();
        verify(notificationService).sendOutOfStockNotification("Watermelon");
        verifyNoMoreInteractions(notificationService);
    }

    private static Product seasonalProduct(int available, int leadTime,
            LocalDate seasonStartDate, LocalDate seasonEndDate) {
        return new Product(null, leadTime, available, "SEASONAL", "Watermelon", null, seasonStartDate, seasonEndDate);
    }
}
