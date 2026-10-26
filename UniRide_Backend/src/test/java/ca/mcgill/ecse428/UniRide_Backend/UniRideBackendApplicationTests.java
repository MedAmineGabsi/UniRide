package ca.mcgill.ecse428.UniRide_Backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UniRideBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
