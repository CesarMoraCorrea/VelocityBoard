package com.example.VelocityBoard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements org.springframework.boot.CommandLineRunner {

    @org.springframework.beans.factory.annotation.Value("${spring.data.mongodb.uri:MISSING}")
    private String uri;

    @Override
    public void run(String... args) {
        System.out.println("====== URI IS: " + uri + " ======");
    }


	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
