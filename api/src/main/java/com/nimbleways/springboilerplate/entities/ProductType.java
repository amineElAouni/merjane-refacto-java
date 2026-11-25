package com.nimbleways.springboilerplate.entities;

import java.util.Arrays;
import java.util.Optional;

public enum ProductType {
    NORMAL,
    SEASONAL,
    EXPIRABLE;

    public static Optional<ProductType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .findFirst();
    }
}
