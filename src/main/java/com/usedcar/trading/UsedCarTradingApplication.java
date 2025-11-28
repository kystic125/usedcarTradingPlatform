package com.usedcar.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UsedCarTradingApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsedCarTradingApplication.class, args);
	}

}
