package io.turntabl.orderservice.models.tickers;

import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "google",createIndex = true)
@NoArgsConstructor
public class Google extends Ticker{

    public Google(String product, String side, double price, int quantity, String exchangeURL) {
        super(product, side, price, quantity, exchangeURL);
    }

}