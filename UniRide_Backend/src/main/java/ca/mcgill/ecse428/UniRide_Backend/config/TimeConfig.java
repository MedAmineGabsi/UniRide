package ca.mcgill.ecse428.UniRide_Backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /**
     * Clock used for time-based rules (e.g. departure at least 1 hour from now).
     * Departure times are entered in local Montréal time, so "now" uses the same zone.
     * Tests can replace this bean with a fixed clock.
     */
    @Bean
    public Clock clock(@Value("${uniride.time-zone:America/Toronto}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
