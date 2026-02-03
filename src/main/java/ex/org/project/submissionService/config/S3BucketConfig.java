package ex.org.project.submissionService.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "s3-bucket")
@Getter
@Setter
public class S3BucketConfig {

    private String inReview;
    private String approved;
    private String sftpIngest;
    private String uploadPortal;

}
