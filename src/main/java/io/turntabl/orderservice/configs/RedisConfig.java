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

    @Bean("create")
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(createOrderListener());
    }

    @Bean
    public CreateOrderListenerImpl createOrderListener() {
        return new CreateOrderListenerImpl();
    }

    @Bean("update")
    public MessageListenerAdapter updateMessageListener() {
        return new MessageListenerAdapter(updateOrderListener());
    }

    @Bean
    public UpdateOrderListenerImpl updateOrderListener() {
        return new UpdateOrderListenerImpl();
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            @Qualifier("create") MessageListenerAdapter listenerAdapter,
                                            @Qualifier("update") MessageListenerAdapter updateListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic());
        container.addMessageListener(updateListenerAdapter, updateOrderTopic());
        return container;
    }

    @Bean("createTopic")
    public ChannelTopic topic() {
        return new ChannelTopic(topic);
    }

    @Bean("updateTopic")
    public ChannelTopic updateOrderTopic() {
        return new ChannelTopic("updateOrder");
    }

}
