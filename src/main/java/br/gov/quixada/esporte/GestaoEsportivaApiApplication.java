package br.gov.quixada.esporte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestaoEsportivaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestaoEsportivaApiApplication.class, args);
	}

}
