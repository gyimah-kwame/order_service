package io.turntabl.orderservice.responses;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.dtos.WalletResponseDto;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class WalletResponse {

    private String id;
    private String email;
    private String fullName;
    private Double balance;
    private List<PortfolioDto> portfolio;

    public static WalletResponse fromDto(WalletResponseDto walletResponseDto) {

        WalletResponse walletResponse = new WalletResponse();

        walletResponse.setEmail(walletResponse.getEmail());
        walletResponse.setBalance(walletResponseDto.getBalance());
        walletResponse.setFullName(walletResponseDto.getFullName());
        walletResponse.setPortfolio(walletResponse.getPortfolio());

        return walletResponse;
    }

}
