package io.turntabl.orderservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UpdateOrderListenerImpl implements MessageListener
{
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("update topic {}", message.toString());
    }
}
