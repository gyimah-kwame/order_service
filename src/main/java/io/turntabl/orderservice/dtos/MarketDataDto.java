package io.turntabl.orderservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketDataDto {

    private String ticker;

    private double sellLimit;

    private double lastTradedPrice;

    private double maxPriceShift;

    private double askPrice;

    private double bidPrice;

    private double buyLimit;

    private String exchangeId;
}
