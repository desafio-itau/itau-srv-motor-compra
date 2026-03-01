package com.itau.srv.motor.compras;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.itau.srv.motor.compras", "com.itau.common.library"})
@EnableFeignClients
public class ServicoDeMotorDeComprasApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory("env")
				.ignoreIfMalformed()
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);
		SpringApplication.run(ServicoDeMotorDeComprasApplication.class, args);
	}

}
