package io.turntabl.orderservice.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class OrderRequest {

    @NotEmpty
    private String side;

    @NotNull
    private Double price;

    @Min(1)
    @NotNull
    private int quantity;

    @NotEmpty
    private String ticker;

}
