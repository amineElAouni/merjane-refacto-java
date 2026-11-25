package com.nimbleways.springboilerplate.services.handlers;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ProductOrderHandlerRegistry {

    private final Map<ProductType, ProductOrderHandler> handlersByType = new EnumMap<>(ProductType.class);

    public ProductOrderHandlerRegistry(List<ProductOrderHandler> handlers) {
        handlers.forEach(handler -> handlersByType.put(handler.supportedType(), handler));
    }

    public Optional<ProductOrderHandler> handlerFor(Product product) {
        return product.productType().map(handlersByType::get);
    }
}
