package com.example.bajaj_hiring;

import com.example.bajaj_hiring.service.HiringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BajajHiringApplication implements CommandLineRunner {

	@Autowired
	private HiringService hiringService;

	public static void main(String[] args) {
		SpringApplication.run(BajajHiringApplication.class, args);
	}

	@Override
	public void run(String... args) {
		hiringService.startProcess();
	}

}
