package com.drawing_compatition.KRastoder;

import org.springframework.boot.SpringApplication;

public class TestKRastoderApplication {

	public static void main(String[] args) {
		SpringApplication.from(KRastoderApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
