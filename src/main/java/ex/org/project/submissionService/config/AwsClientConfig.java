package ex.org.project.submissionService.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.macie2.AmazonMacie2;
import com.amazonaws.services.macie2.AmazonMacie2Client;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.sfn.SfnClient;

@Configuration
public class AwsClientConfig {

    @Value("${server.max-file-size-MB}")
    private String maxFileSize;
    @Value("${server.max-request-size-MB}")
    private String maxRequestSize;

    @Bean
    public MultipartConfigElement multipartConfigElement(){
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(Long.parseLong(maxFileSize)));
        factory.setMaxRequestSize(DataSize.ofMegabytes(Long.parseLong(maxRequestSize)));
        return factory.createMultipartConfig();
    }

    @Bean
    public AmazonMacie2 amazonMacie2Client(){
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        //set max retries to 20, backoff strategy is base delay of 10 second and max backoff time 30 seconds
        //full jitter strategy prevents message collision between clients, randomly selects backoff time and reduces retries
        clientConfiguration.setMaxConsecutiveRetriesBeforeThrottling(20);
        clientConfiguration.setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                new PredefinedBackoffStrategies.FullJitterBackoffStrategy(10000, 30000), 20, true));
        return AmazonMacie2Client.builder().withClientConfiguration(clientConfiguration).build();
    }

    @Bean
    public SfnClient sfnClient(){
        return SfnClient.create();
    }
}
