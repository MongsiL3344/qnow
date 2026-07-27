package io.github.mongsil3344.qnow.user.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "qnow.email-verification", name = "sender", havingValue = "ses")
public class SesEmailVerificationConfig {

    @Bean
    SesClient sesClient(@Value("${aws.region}") String region) {
        return SesClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
