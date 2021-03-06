package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.exceptions.WalletNotFoundException;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.services.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public WalletDto createWallet(String userId) {
        log.info("user id a {}", userId);
        Optional<Wallet> userWallet = walletRepository.findById(userId);
        if (userWallet.isPresent()) {
            return  WalletDto.fromModel(userWallet.get());
        }
        Wallet wallet = walletRepository.save(new Wallet(userId, 10_000.00, new ArrayList<>()));
        log.info("Returning Wallet Information for {}", userId);
        return WalletDto.fromModel(wallet);
    }

    @Override
    public List<PortfolioDto> getUserPortfolios(String userId) {
        Optional<Wallet> userWallet = walletRepository.findById(userId);
        Wallet wallet = userWallet.orElseThrow(() -> new WalletNotFoundException("User's wallet does not exist",userId));
        return wallet.getPortfolios();
    }

    @Override
    public List<WalletDto> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(WalletDto::fromModel).collect(Collectors.toList());
    }
}
