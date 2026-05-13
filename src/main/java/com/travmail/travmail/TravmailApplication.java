package com.travmail.travmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravmailApplication.class, args);
	}

}
