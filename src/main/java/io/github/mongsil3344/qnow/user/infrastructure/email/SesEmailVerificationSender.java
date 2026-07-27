package io.github.mongsil3344.qnow.user.infrastructure.email;

import io.github.mongsil3344.qnow.user.application.EmailVerificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Component
@ConditionalOnProperty(prefix = "qnow.email-verification", name = "sender", havingValue = "ses")
public class SesEmailVerificationSender implements EmailVerificationSender {

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailVerificationSender(
        SesClient sesClient,
        @Value("${qnow.email-verification.from-email}") String fromEmail
    ) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String email, String code) {
        Content subject = Content.builder()
            .charset("UTF-8")
            .data("[Nowslide] 이메일 인증번호")
            .build();
        Content text = Content.builder()
            .charset("UTF-8")
            .data("Nowslide 회원가입 인증번호는 %s입니다.".formatted(code))
            .build();

        sesClient.sendEmail(SendEmailRequest.builder()
            .source(fromEmail)
            .destination(Destination.builder().toAddresses(email).build())
            .message(Message.builder()
                .subject(subject)
                .body(Body.builder().text(text).build())
                .build())
            .build());
    }
}
