package io.turntabl.orderservice.responses;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Execution {

    private LocalDateTime timestamp;

    private double price;

    private int quantity;

}
