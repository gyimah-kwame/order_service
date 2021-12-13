package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.Microsoft;
import io.turntabl.orderservice.models.tickers.Oracle;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OracleRepository extends ElasticsearchRepository<Oracle, String> {
    List<Oracle> findTop25BySideOrderByPriceAsc(String side);
    List<Oracle> findTop5BySideOrderByPriceDesc(String side);
}
