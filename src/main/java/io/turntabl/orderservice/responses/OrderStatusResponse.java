package io.turntabl.orderservice.responses;

import lombok.Data;

@Data
public class OrderStatusResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
