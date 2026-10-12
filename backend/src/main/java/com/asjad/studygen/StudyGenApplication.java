package com.asjad.studygen;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class StudyGenApplication {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		// Load .env from current directory
		Dotenv.configure().ignoreIfMissing().systemProperties().load();
		// If running inside backend/ directory, also load common .env from parent root
		try {
			java.io.File parentEnv = new java.io.File("../.env");
			if (parentEnv.exists()) {
				Dotenv.configure().directory("../").ignoreIfMissing().systemProperties().load();
			}
		} catch (Exception ignored) {
		}

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(StudyGenApplication.class, args);
	}
}