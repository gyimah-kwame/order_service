package io.turntabl.orderservice.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MalonOrderStatusResponse {

    private String product;

    private int quantity;

    private double price;

    private String side;

    private List<Execution> executions;

    @JsonProperty("cumulatitiveQuantity")
    private int cumulativeQuantity;

}
