package com.nimbleways.springboilerplate.entities;

import lombok.*;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lead_time")
    private Integer leadTime;

    @Column(name = "available")
    private Integer available;

    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "season_start_date")
    private LocalDate seasonStartDate;

    @Column(name = "season_end_date")
    private LocalDate seasonEndDate;

    public Optional<ProductType> productType() {
        return ProductType.fromCode(type);
    }

    public boolean isInStock() {
        return available > 0;
    }

    public void decrementStock() {
        available = available - 1;
    }

    public void emptyStock() {
        available = 0;
    }

    public boolean hasLeadTime() {
        return leadTime > 0;
    }

    public boolean isInSeason(LocalDate today) {
        return today.isAfter(seasonStartDate) && today.isBefore(seasonEndDate);
    }

    public boolean isBeforeSeason(LocalDate today) {
        return seasonStartDate.isAfter(today);
    }

    public boolean isRestockedAfterSeasonEnd(LocalDate today) {
        return today.plusDays(leadTime).isAfter(seasonEndDate);
    }

    public boolean isExpired(LocalDate today) {
        return !expiryDate.isAfter(today);
    }
}
