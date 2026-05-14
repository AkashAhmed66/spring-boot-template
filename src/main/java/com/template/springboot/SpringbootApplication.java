package com.template.springboot;

import com.template.springboot.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringbootApplication {

	public static void main(String[] args) {
		// Load .env into System.properties BEFORE Spring starts. Done here (instead of via
		// EnvironmentPostProcessor) because Spring Boot 4 has been flaky about the SPI —
		// running it directly in main() makes the path impossible to miss.
		DotenvLoader.load();
		SpringApplication.run(SpringbootApplication.class, args);
	}

}
