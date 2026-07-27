package io.github.mongsil3344.qnow.user.application;

import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.CREATED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.CODE_MISMATCH;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.DELIVERY_FAILED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationStore verificationStore;

    @Mock
    private EmailVerificationSender verificationSender;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, verificationStore, verificationSender);
    }

    @Test
    void 인증번호를_발급하면_이메일을_정규화하고_6자리_코드를_전송한다() {
        String email = "user@example.com";
        when(verificationStore.createRequest(eq(email), anyString())).thenReturn(CREATED);

        service.requestCode(" User@Example.com ");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationStore).createRequest(eq(email), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        verify(verificationSender).send(email, codeCaptor.getValue());
    }

    @Test
    void 이메일_전송에_실패하면_예약한_인증요청을_취소한다() {
        String email = "user@example.com";
        when(verificationStore.createRequest(eq(email), anyString())).thenReturn(CREATED);
        doThrow(new IllegalStateException("SES unavailable"))
            .when(verificationSender).send(eq(email), anyString());

        assertThatThrownBy(() -> service.requestCode(email))
            .isInstanceOfSatisfying(EmailVerificationException.class, exception ->
                assertThat(exception.error()).isEqualTo(DELIVERY_FAILED)
            );

        ArgumentCaptor<String> createdCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cancelledCode = ArgumentCaptor.forClass(String.class);
        verify(verificationStore).createRequest(eq(email), createdCode.capture());
        verify(verificationStore).cancelRequest(eq(email), cancelledCode.capture());
        assertThat(cancelledCode.getValue()).isEqualTo(createdCode.getValue());
    }

    @Test
    void 틀린_인증번호는_도메인_오류로_변환한다() {
        when(verificationStore.verify("user@example.com", "123456")).thenReturn(CODE_MISMATCH);

        assertThatThrownBy(() -> service.verifyCode("user@example.com", "123456"))
            .isInstanceOfSatisfying(EmailVerificationException.class, exception ->
                assertThat(exception.error()).isEqualTo(EmailVerificationException.Error.CODE_MISMATCH)
            );
    }

    @Test
    void 인증되지_않은_이메일은_회원가입에_사용할_수_없다() {
        when(verificationStore.isVerified("user@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.requireVerified("user@example.com"))
            .isInstanceOfSatisfying(EmailVerificationException.class, exception ->
                assertThat(exception.error()).isEqualTo(REQUIRED)
            );

        verifyNoInteractions(verificationSender);
    }
}
