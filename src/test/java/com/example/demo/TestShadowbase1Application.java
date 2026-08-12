package com.example.demo;

import org.springframework.boot.SpringApplication;

public class TestShadowbase1Application {

	public static void main(String[] args) {
		SpringApplication.from(Shadowbase1Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
