package com.nimbleways.springboilerplate.entities;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ProductTests {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 15);

    @Nested
    @DisplayName("stock")
    class Stock {

        @Test
        void isInStockWhenAtLeastOneUnitIsAvailable() {
            assertThat(productWithStock(1).isInStock()).isTrue();
            assertThat(productWithStock(0).isInStock()).isFalse();
        }

        @Test
        void decrementStockRemovesOneUnit() {
            Product product = productWithStock(30);

            product.decrementStock();

            assertThat(product.getAvailable()).isEqualTo(29);
        }

        @Test
        void emptyStockLeavesNoUnitAvailable() {
            Product product = productWithStock(30);

            product.emptyStock();

            assertThat(product.getAvailable()).isZero();
        }

        @Test
        void hasLeadTimeWhenRestockingTakesAtLeastOneDay() {
            assertThat(productWithLeadTime(1).hasLeadTime()).isTrue();
            assertThat(productWithLeadTime(0).hasLeadTime()).isFalse();
        }
    }

    @Nested
    @DisplayName("season")
    class Season {

        @Test
        void isInSeasonStrictlyBetweenSeasonStartAndEnd() {
            assertThat(seasonalProduct(TODAY.minusDays(1), TODAY.plusDays(1)).isInSeason(TODAY)).isTrue();
        }

        @Test
        @DisplayName("legacy: first and last days of season are not in season")
        void legacyIsNotInSeasonOnSeasonBoundaries() {
            assertThat(seasonalProduct(TODAY, TODAY.plusDays(10)).isInSeason(TODAY)).isFalse();
            assertThat(seasonalProduct(TODAY.minusDays(10), TODAY).isInSeason(TODAY)).isFalse();
        }

        @Test
        void isBeforeSeasonUntilTheDayBeforeSeasonStart() {
            assertThat(seasonalProduct(TODAY.plusDays(1), TODAY.plusDays(10)).isBeforeSeason(TODAY)).isTrue();
            assertThat(seasonalProduct(TODAY, TODAY.plusDays(10)).isBeforeSeason(TODAY)).isFalse();
        }

        @Test
        void isRestockedAfterSeasonEndWhenLeadTimeGoesPastTheLastDay() {
            LocalDate seasonEnd = TODAY.plusDays(10);

            assertThat(seasonalProduct(11, TODAY.minusDays(1), seasonEnd).isRestockedAfterSeasonEnd(TODAY)).isTrue();
            assertThat(seasonalProduct(10, TODAY.minusDays(1), seasonEnd).isRestockedAfterSeasonEnd(TODAY)).isFalse();
        }
    }

    @Nested
    @DisplayName("expiration")
    class Expiration {

        @Test
        void isNotExpiredBeforeItsExpiryDate() {
            assertThat(expirableProduct(TODAY.plusDays(1)).isExpired(TODAY)).isFalse();
        }

        @Test
        void isExpiredFromItsExpiryDate() {
            assertThat(expirableProduct(TODAY).isExpired(TODAY)).isTrue();
            assertThat(expirableProduct(TODAY.minusDays(1)).isExpired(TODAY)).isTrue();
        }
    }

    @Nested
    @DisplayName("type")
    class Type {

        @Test
        void resolvesKnownProductTypes() {
            assertThat(productOfType("NORMAL").productType()).contains(ProductType.NORMAL);
            assertThat(productOfType("SEASONAL").productType()).contains(ProductType.SEASONAL);
            assertThat(productOfType("EXPIRABLE").productType()).contains(ProductType.EXPIRABLE);
        }

        @Test
        void hasNoProductTypeWhenTypeIsUnknown() {
            assertThat(productOfType("FLASHSALE").productType()).isEmpty();
        }
    }

    private static Product productWithStock(int available) {
        return new Product(null, 15, available, "NORMAL", "USB Cable", null, null, null);
    }

    private static Product productWithLeadTime(int leadTime) {
        return new Product(null, leadTime, 0, "NORMAL", "USB Cable", null, null, null);
    }

    private static Product seasonalProduct(LocalDate seasonStartDate, LocalDate seasonEndDate) {
        return seasonalProduct(15, seasonStartDate, seasonEndDate);
    }

    private static Product seasonalProduct(int leadTime, LocalDate seasonStartDate, LocalDate seasonEndDate) {
        return new Product(null, leadTime, 30, "SEASONAL", "Watermelon", null, seasonStartDate, seasonEndDate);
    }

    private static Product expirableProduct(LocalDate expiryDate) {
        return new Product(null, 15, 30, "EXPIRABLE", "Milk", expiryDate, null, null);
    }

    private static Product productOfType(String type) {
        return new Product(null, 15, 30, type, "Any product", null, null, null);
    }
}
