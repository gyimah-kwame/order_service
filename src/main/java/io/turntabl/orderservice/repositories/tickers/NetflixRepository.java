package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.Microsoft;
import io.turntabl.orderservice.models.tickers.Netflix;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetflixRepository extends ElasticsearchRepository<Netflix, String> {
    List<Netflix> findTop25BySideOrderByPriceAsc(String side);
    List<Netflix> findTop5BySideOrderByPriceDesc(String side);
}
