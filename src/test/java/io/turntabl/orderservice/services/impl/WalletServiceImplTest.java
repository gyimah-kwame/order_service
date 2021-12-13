package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.exceptions.WalletNotFoundException;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.services.WalletService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Wallet Service Tests")
@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    WalletRepository walletRepository;

    @InjectMocks
    WalletServiceImpl walletService;

    @Test
    @DisplayName("Test Return wallet information if user exists")
    void createWallet() {

        Wallet wallet = new Wallet("userID", 12.00, new ArrayList<>());
        Mockito.when(walletRepository.findById("userID"))
                .thenReturn(Optional.of(wallet));

        WalletDto walletReturned = walletService.createWallet("userID");
        Assertions.assertThat(wallet.getBalance()).isEqualTo(walletReturned.getBalance());

    }
    @Test
    @DisplayName("Test Create Wallet for new User if wallet does not exist")
    void createWalletWhenUserIsNew() {

        Mockito.when(walletRepository.save(ArgumentMatchers.any()))
                .thenReturn(new Wallet(ArgumentMatchers.anyString(), 10_000.00, new ArrayList<>()));

        WalletDto walletReturned = walletService.createWallet("userID");
        Assertions.assertThat(10_000.00).isEqualTo(walletReturned.getBalance());

    }

    @Test
    @DisplayName("Test Get User Portfolio for valid user")
    void getUserPortfolios() {
        Mockito.when(walletRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new Wallet("UseriD",10_000.00, List.of(new PortfolioDto("AMZ",10)))));
        Assertions.assertThat(1).isEqualTo(walletService.getUserPortfolios(ArgumentMatchers.anyString()).size());
        Assertions.assertThat("AMZ").isEqualTo(walletService.getUserPortfolios(ArgumentMatchers.anyString()).get(0).getTicker());
    }
    @Test
    @DisplayName("Test Get User Portfolio for Invalid user")
    void throwExceptionWhenUserWalletDoesNotExist() {
        Mockito.when(walletRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(WalletNotFoundException.class)
                .isThrownBy(() -> walletService.getUserPortfolios(ArgumentMatchers.anyString()))
                .withMessage("User's wallet does not exist");
    }
}
