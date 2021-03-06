package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.Google;
import io.turntabl.orderservice.models.tickers.Microsoft;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoogleRepository extends ElasticsearchRepository<Google, String> {
    List<Google> findTop25BySideOrderByPriceAsc(String side);
    List<Google> findTop5BySideOrderByPriceDesc(String side);
}
