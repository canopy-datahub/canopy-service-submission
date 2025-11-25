package ex.org.project.submissionService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = {
	"ex.org.project.submissionService.repositories",  // Submission service repositories
	"ex.org.project.datahub.auth.repository"          // Keycloak library repositories
})
@EntityScan(basePackages = {
	"ex.org.project.submissionService.models",        // Submission service entities
	"ex.org.project.datahub.auth.model"               // Keycloak library entities
})
public class SubmissionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubmissionServiceApplication.class, args);
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

}
