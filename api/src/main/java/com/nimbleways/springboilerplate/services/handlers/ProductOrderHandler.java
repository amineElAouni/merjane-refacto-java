package com.nimbleways.springboilerplate.services.handlers;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

import java.time.LocalDate;

public interface ProductOrderHandler {

    ProductType supportedType();

    void handle(Product product, LocalDate today);
}
