package Clinic_Management.PatientIntakeService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PatientIntakeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientIntakeServiceApplication.class, args);
	}

}