package io.turntabl.orderservice.repositories.tickers;


import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.Microsoft;
import io.turntabl.orderservice.models.tickers.Tesla;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeslaRepository extends ElasticsearchRepository<Tesla, String> {
    List<Tesla> findTop25BySideOrderByPriceAsc(String side);
    List<Microsoft> findTop5BySideOrderByPriceDesc(String side);
}
