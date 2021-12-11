package io.turntabl.orderservice.configs;

import io.turntabl.orderservice.services.impl.CreateOrderListenerImpl;
import io.turntabl.orderservice.services.impl.UpdateOrderListenerImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    @Value("${redis.topic}")
    private String topic;

    @Bean
    public StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean("createOrderMessageListener")
    public MessageListenerAdapter createOrderMessageListener() {
        return new MessageListenerAdapter(createOrderListener());
    }

    @Bean
    public CreateOrderListenerImpl createOrderListener() {
        return new CreateOrderListenerImpl();
    }

    @Bean
    public UpdateOrderListenerImpl updateOrderListener() {
        return new UpdateOrderListenerImpl();
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            @Qualifier("createOrderMessageListener") MessageListenerAdapter createOrderMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(createOrderMessageListener, createOrderTopic());
//        container.addMessageListener(updateListenerAdapter, updateOrderTopic());
        return container;
    }

    @Bean("createOrderTopic")
    public ChannelTopic createOrderTopic() {
        return new ChannelTopic(topic);
    }

}
