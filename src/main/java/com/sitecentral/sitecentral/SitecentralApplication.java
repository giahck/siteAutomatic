package com.sitecentral.sitecentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SitecentralApplication {

	public static void main(String[] args) {
		SpringApplication.run(SitecentralApplication.class, args);
	}

}
