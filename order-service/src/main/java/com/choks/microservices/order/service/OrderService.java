package com.choks.microservices.order.service;

import com.choks.microservices.order.client.InventoryClient;
import com.choks.microservices.order.dto.OrderRequest;
import com.choks.microservices.order.event.OrderPlacedEvent;
import com.choks.microservices.order.model.Order;
import com.choks.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest) {

        var isProductInStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

        if(isProductInStock) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price());
            order.setSkuCode(orderRequest.skuCode());
            order.setQuantity(orderRequest.quantity());

            orderRepository.save(order);

            OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent();
                    orderPlacedEvent.setOrderNumber(order.getOrderNumber());
                    orderPlacedEvent.setEmail(orderRequest.userDetails().email());
                    orderPlacedEvent.setFirstName(orderRequest.userDetails().firstName());
                    orderPlacedEvent.setLastName(orderRequest.userDetails().lastName());
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@ "+ orderRequest.userDetails().email());
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@ "+ orderRequest.userDetails().firstName());
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@ "+ orderRequest.userDetails().lastName());
            log.info("Start - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
            kafkaTemplate.send("order-placed", orderPlacedEvent);
            log.info("End - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);

        } else {
            throw new RuntimeException("Product with SkuCode " + orderRequest.skuCode() + " is not in stock");
        }


    }
}
