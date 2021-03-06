package io.turntabl.orderservice.repositories;

import io.turntabl.orderservice.models.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findByIdAndUserIdOrderByCreatedAtDesc(String id, String userId);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    List<Order> findByStatusOrderByCreatedAtDesc(String status);

}
