package com.electronyoon.tableorder;

import org.springframework.boot.SpringApplication;

public class TestTableOrderBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(TableOrderBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
