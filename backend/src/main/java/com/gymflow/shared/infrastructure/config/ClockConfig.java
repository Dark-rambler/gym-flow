package com.gymflow.shared.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Reloj inyectable: los casos de uso usan Clock (nunca LocalDate.now()) para que los tests puedan mover el tiempo.
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
