package ca.mcgill.ecse428.UniRide_Backend;

import org.springframework.boot.SpringApplication;

public class TestUniRideBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(UniRideBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
