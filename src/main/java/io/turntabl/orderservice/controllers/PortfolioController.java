package io.turntabl.orderservice.controllers;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.services.WalletService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("/api/v1")
public class PortfolioController {

    private final WalletService walletService;

    @GetMapping("/portfolios")
    public List<PortfolioDto> getUserPortfolios(@AuthenticationPrincipal Jwt principal) {
        return walletService.getUserPortfolios(principal.getSubject());
    }
}
