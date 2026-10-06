package com.asjad.studygen;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class StudyGenApplication {

	public static void main(String[] args) {
		Dotenv.configure().ignoreIfMissing().systemProperties().load();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(StudyGenApplication.class, args);
	}
}