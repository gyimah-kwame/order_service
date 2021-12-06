package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.services.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public WalletDto createWallet(String userId) {
        Optional<Wallet> userWallet = walletRepository.findByUserId(userId);
        Wallet wallet = userWallet.orElse(new Wallet(userId, 10_000.00, new ArrayList<>()));
        log.info("Returning Wallet Information for {}", userId);
        return WalletDto.fromModel(walletRepository.save(wallet));
    }
}