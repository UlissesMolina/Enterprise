package com.example.expense_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ExpenseApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseApiApplication.class, args);
	}

}
