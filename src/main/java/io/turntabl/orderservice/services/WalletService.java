package io.turntabl.orderservice.services;

import io.turntabl.orderservice.dtos.WalletDto;
import org.springframework.stereotype.Component;

@Component
public interface WalletService {

    WalletDto createWallet(String userId);
}
