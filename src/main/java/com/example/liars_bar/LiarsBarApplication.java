package com.example.liars_bar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LiarsBarApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiarsBarApplication.class, args);
	}

}
