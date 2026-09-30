package com.gymflow.support;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

    /** 10 de marzo de 2026, 10:00 en Lima (15:00 UTC). ApiTestSupport lo reinicia antes de cada test. */
    public static final Instant START = Instant.parse("2026-03-10T15:00:00Z");

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(START);
    }
}
