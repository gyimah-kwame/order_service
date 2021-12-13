package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.dtos.ExchangeDto;
import io.turntabl.orderservice.dtos.MarketDataDto;
import io.turntabl.orderservice.enums.ExchangeName;
import io.turntabl.orderservice.services.RedisService;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final Gson gson;
    private final HashOperations<String, String, String> hashOperations;
    private final StringRedisTemplate stringRedisTemplate;
    private final PatternTopic createOrderTopic;
    private final PatternTopic updateOrderTopic;


    @Override
    public void convertAndSendToCreateOrderTopic(String orderId){
        stringRedisTemplate.convertAndSend(createOrderTopic.getTopic(), orderId);
    }

    @Override
    public void convertAndSendToUpdateOrderTopic(String orderId){
        stringRedisTemplate.convertAndSend(updateOrderTopic.getTopic(), orderId);
    }

    @Override
    public ExchangeDto getExchangeDetailsFromHash(ExchangeName exchangeName){
        String exchangeInfo = hashOperations.get(exchangeName.name(),exchangeName.name());
        return gson.fromJson(exchangeInfo, ExchangeDto.class);
    }

    @Override
    public MarketDataDto getMarketDataFromHash(String ticker, String exchangeId){
        String dataKey = ticker+"_"+exchangeId;
        return gson.fromJson(hashOperations.get(dataKey, dataKey), MarketDataDto.class);
    }


}
