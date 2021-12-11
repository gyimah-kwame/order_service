package io.turntabl.orderservice.repositories.tickers;

import io.turntabl.orderservice.models.tickers.Amazon;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmazonRepository extends ElasticsearchRepository<Amazon, String> {

    List<Amazon> findBySideOrderByPriceAsc(String side);
}
