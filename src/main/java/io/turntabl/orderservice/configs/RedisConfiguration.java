package io.turntabl.orderservice.configs;

import io.turntabl.orderservice.services.impl.RedisCreateOrderReceiver;
import io.turntabl.orderservice.services.impl.RedisUpdateOrderReceiver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfiguration {


    @Bean
    public RedisMessageListenerContainer container(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("createOrderMessageAdapter") MessageListenerAdapter createOrderMessageAdapter,
            @Qualifier("updateOrderMessageAdapter") MessageListenerAdapter updateOrderMessageAdapter
    ){
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);
        listenerContainer.addMessageListener(createOrderMessageAdapter,createOrderTopic());
        listenerContainer.addMessageListener(updateOrderMessageAdapter,updateOrderTopic());

        return listenerContainer;
    }

    @Bean("createOrderMessageAdapter")
    public MessageListenerAdapter createOrderMessageAdapter()
    {
        return new MessageListenerAdapter(redisCreateOrderReceiver(), "createOrderMessageConsumer");
    }

    @Bean("updateOrderMessageAdapter")
    public MessageListenerAdapter updateOrderMessageAdapter()
    {
        return new MessageListenerAdapter(redisUpdateOrderReceiver(), "updateOrderMessageConsumer");
    }

    @Bean
    public PatternTopic createOrderTopic() {
        return new PatternTopic("turntabl.io:createorder");
    }

    @Bean
    public PatternTopic updateOrderTopic() {
        return new PatternTopic("turntabl.io:updateorder");
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String,String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    RedisCreateOrderReceiver redisCreateOrderReceiver() {
        return new RedisCreateOrderReceiver();
    }

    @Bean
    RedisUpdateOrderReceiver redisUpdateOrderReceiver(){
        return new RedisUpdateOrderReceiver();
    }

}

