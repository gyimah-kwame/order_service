package io.turntabl.orderservice.controllers;

import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.services.WalletService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
@AllArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallets")
    @ResponseStatus(code = HttpStatus.CREATED)
    public WalletDto createWallet(@AuthenticationPrincipal Jwt principal) {
        return walletService.createWallet(principal.getSubject()) ;
    }
}
