package io.turntabl.orderservice.schedulers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
@AllArgsConstructor
public class CheckOrderStatusScheduler {

    private final WebClient webClient;

    @Value("${matraining.token}")
    private String apiKey;

    @Scheduled(cron = "*/2 * * * * *")
    public void checkOrderStatus() {
        log.info("logging");
    }
}
