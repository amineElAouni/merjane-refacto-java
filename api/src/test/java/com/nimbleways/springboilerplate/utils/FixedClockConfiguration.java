package com.nimbleways.springboilerplate.utils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixedClockConfiguration {

    public static final LocalDate TODAY = LocalDate.of(2024, 6, 15);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }
}
