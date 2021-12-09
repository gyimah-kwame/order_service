package io.turntabl.orderservice.repositories;

import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.OrderBook;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;


public interface OrderBookRepository extends ElasticsearchRepository<OrderBook, String> {
    List<OrderBook> findFirst100ByProductAndSideOrderByPriceAscLocalDateTimeDesc(String product,String side);
}
