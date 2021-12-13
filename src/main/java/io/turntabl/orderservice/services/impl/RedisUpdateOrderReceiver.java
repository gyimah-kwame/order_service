package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;


public class RedisUpdateOrderReceiver {

    @Autowired
    private Gson gson;


    /**
     * Processes Incoming Message Converted to Object from MTN
     * @param message
     */
    public void updateOrderMessageConsumer(String message){

    }

}
