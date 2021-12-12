package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.IBM;
import io.turntabl.orderservice.models.tickers.Microsoft;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBMRepository extends ElasticsearchRepository<IBM, String> {
    List<IBM> findTop25BySideOrderByPriceAsc(String side);
    List<IBM> findTop5BySideOrderByPriceDesc(String side);
}
