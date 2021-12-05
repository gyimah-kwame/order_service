package io.turntabl.orderservice.services.impl;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class UpdateOrderListenerImpl implements MessageListener
{
    @Override
    public void onMessage(Message message, byte[] pattern) {

    }
}
