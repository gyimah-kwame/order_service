package io.turntabl.orderservice.models;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.dtos.WalletDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document("wallets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {

    @Id
    @Field("user_id")
    private String userId;

    private double balance = 10_000.00;

    private List<PortfolioDto> portfolios;

}
