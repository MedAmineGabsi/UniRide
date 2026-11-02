package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the app's Clock with a {@link TestClock} in Cucumber tests. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

    @Bean
    @Primary
    public TestClock testClock(@Value("${uniride.time-zone:America/Toronto}") String zone) {
        return new TestClock(ZoneId.of(zone));
    }
}
