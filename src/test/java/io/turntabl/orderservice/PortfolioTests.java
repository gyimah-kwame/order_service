package io.turntabl.orderservice;

import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.services.WalletService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class PortfolioTests {

    private final PortfolioDto portfolioDto = new PortfolioDto("IBM", 10);
    private final PortfolioDto portfolioDto2 = new PortfolioDto("MSFT", 20);

    @Mock
    private WalletService walletService;


    @Test
    public void testGetUserPortfolios() {

        Mockito.when(walletService.getUserPortfolios("1")).thenReturn(new ArrayList<>(List.of(portfolioDto, portfolioDto2)));
        Assertions.assertEquals(2, walletService.getUserPortfolios("1").size());

    }
}
