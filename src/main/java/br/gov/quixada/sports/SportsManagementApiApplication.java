package br.gov.quixada.sports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsManagementApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportsManagementApiApplication.class, args);
	}

}
