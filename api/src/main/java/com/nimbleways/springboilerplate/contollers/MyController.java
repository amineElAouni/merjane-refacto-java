package com.nimbleways.springboilerplate.contollers;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.ProductService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class MyController {
    @Autowired
    private ProductService ps;

    @Autowired
    private ProductRepository pr;

    @Autowired
    private OrderRepository or;

    @Autowired
    private Clock clock;

    @PostMapping("{orderId}/processOrder")
    @ResponseStatus(HttpStatus.OK)
    public ProcessOrderResponse processOrder(@PathVariable Long orderId) {
        Order order = or.findById(orderId).get();
        System.out.println(order);
        List<Long> ids = new ArrayList<>();
        ids.add(orderId);
        Set<Product> products = order.getItems();
        LocalDate today = LocalDate.now(clock);
        for (Product p : products) {
            if (p.getType().equals("NORMAL")) {
                if (p.isInStock()) {
                    p.decrementStock();
                    pr.save(p);
                } else if (p.hasLeadTime()) {
                    ps.notifyDelay(p.getLeadTime(), p);
                }
            } else if (p.getType().equals("SEASONAL")) {
                if (p.isInSeason(today) && p.isInStock()) {
                    p.decrementStock();
                    pr.save(p);
                } else {
                    ps.handleSeasonalProduct(p);
                }
            } else if (p.getType().equals("EXPIRABLE")) {
                if (p.isInStock() && !p.isExpired(today)) {
                    p.decrementStock();
                    pr.save(p);
                } else {
                    ps.handleExpiredProduct(p);
                }
            }
        }

        return new ProcessOrderResponse(order.getId());
    }
}
