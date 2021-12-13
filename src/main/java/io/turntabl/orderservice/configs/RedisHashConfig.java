package io.turntabl.orderservice.configs;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@AllArgsConstructor
public class RedisHashConfig {

    private final StringRedisTemplate stringRedisTemplate;

    @Bean
    public HashOperations<String, String, String> hashOperations() {
        return stringRedisTemplate.opsForHash();
    }
}
