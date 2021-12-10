package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import io.turntabl.orderservice.models.tickers.Microsoft;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MicrosoftRepository extends ElasticsearchRepository<Microsoft, String> {
    List<Microsoft> findTop25BySideOrderByPriceAsc(String side);
}
