package io.turntabl.orderservice.repositories;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.models.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserId(String userId);

    Optional<Order> findByIdAndUserId(String id, String userId);

    List<Order> findByUserIdAndStatus(String userId, String status);

    List<Order> findByStatus(String status);

}
