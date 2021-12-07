package io.turntabl.orderservice.services;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.dtos.WalletDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface WalletService {

    WalletDto createWallet(String userId);

    List<PortfolioDto> getUserPortfolios(String userId);
}
