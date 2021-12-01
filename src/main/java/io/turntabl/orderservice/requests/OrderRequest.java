package io.turntabl.orderservice.requests;

import io.turntabl.orderservice.constants.Side;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Side side;

    @NotNull
    private Double price;

    @Min(1)
    @NotNull
    private int quantity;

    @NotBlank
    private String ticker;

}
