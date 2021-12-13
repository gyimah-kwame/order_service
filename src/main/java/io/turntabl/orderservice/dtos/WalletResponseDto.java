package io.turntabl.orderservice.dtos;


import io.turntabl.orderservice.models.Wallet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponseDto implements Serializable {

    private String id;
    private double email;
    private String fullName;
    private Double balance;
    private List<PortfolioDto> portfolios;


}
