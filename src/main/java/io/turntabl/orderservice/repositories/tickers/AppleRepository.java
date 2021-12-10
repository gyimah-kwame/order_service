package io.turntabl.orderservice.repositories.tickers;


import io.turntabl.orderservice.models.tickers.Apple;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppleRepository extends ElasticsearchRepository<Apple, String> {
    List<Apple> findBySideOrderByPriceAsc(String side);
}
