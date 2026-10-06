package com.asjad.studygen;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StudyGenApplicationTests {

	@BeforeAll
	static void setup() {
		Dotenv.configure().ignoreIfMissing().systemProperties().load();
	}

	@Test
	void contextLoads() {
	}

}
