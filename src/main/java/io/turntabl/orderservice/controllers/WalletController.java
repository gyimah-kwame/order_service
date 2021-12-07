package io.turntabl.orderservice.controllers;

import io.turntabl.orderservice.dtos.WalletDto;
import io.turntabl.orderservice.services.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/")
@CrossOrigin
@AllArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallets")
    @ResponseStatus(code = HttpStatus.CREATED)
    public WalletDto createWallet(@AuthenticationPrincipal Jwt principal) {
        log.info("Getting Information for {}",principal.getSubject());
        return walletService.createWallet(principal.getSubject()) ;
    }
}
