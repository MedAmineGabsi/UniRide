package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import ca.mcgill.ecse428.UniRide_Backend.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest
@Import({ TestcontainersConfiguration.class, TestClockConfiguration.class })
public class CucumberSpringConfig {
}
