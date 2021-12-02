package io.turntabl.orderservice.dtos;

import io.turntabl.orderservice.models.Wallet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDto {

    private String userId;

    private double balance;

    private List<PortfolioDto> portfolios;

    public static WalletDto fromModel(Wallet wallet) {
        WalletDto walletDto = new WalletDto();
        walletDto.setBalance(wallet.getBalance());
        walletDto.setUserId(wallet.getUserId());
        walletDto.setPortfolios(wallet.getPortfolios());

        return walletDto;
    }
}
