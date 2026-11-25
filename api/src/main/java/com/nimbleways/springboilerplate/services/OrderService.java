package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.handlers.ProductOrderHandlerRegistry;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductOrderHandlerRegistry productOrderHandlers;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
            ProductOrderHandlerRegistry productOrderHandlers, Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productOrderHandlers = productOrderHandlers;
        this.clock = clock;
    }

    @Transactional
    public Order processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        order.getItems().forEach(product -> processProduct(product, today));
        return order;
    }

    private void processProduct(Product product, LocalDate today) {
        productOrderHandlers.handlerFor(product).ifPresent(handler -> {
            handler.handle(product, today);
            productRepository.save(product);
        });
    }
}
