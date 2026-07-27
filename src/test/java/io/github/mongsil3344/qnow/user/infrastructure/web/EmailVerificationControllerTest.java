package io.github.mongsil3344.qnow.user.infrastructure.web;

import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.CREATED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.TOO_FREQUENT;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.user.application.EmailVerificationSender;
import io.github.mongsil3344.qnow.user.application.EmailVerificationStore;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailVerificationStore verificationStore;

    @MockitoBean
    private EmailVerificationSender verificationSender;

    @Test
    void 인증번호를_요청하면_6자리_코드를_전송한다() throws Exception {
        String email = uniqueEmail("request");
        when(verificationStore.createRequest(eq(email), anyString())).thenReturn(CREATED);

        mockMvc.perform(post("/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s"}
                    """.formatted(email)))
            .andExpect(status().isAccepted());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationSender).send(eq(email), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void cooldown_중_반복_요청은_요청과다_응답을_반환한다() throws Exception {
        String email = uniqueEmail("cooldown");
        when(verificationStore.createRequest(eq(email), anyString())).thenReturn(TOO_FREQUENT);

        mockMvc.perform(post("/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s"}
                    """.formatted(email)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT"));
    }

    @Test
    void 정확한_인증번호를_확인하면_성공한다() throws Exception {
        String email = uniqueEmail("confirm");
        when(verificationStore.verify(email, "123456")).thenReturn(VERIFIED);

        mockMvc.perform(post("/email-verifications/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "verificationCode":"123456"
                    }
                    """.formatted(email)))
            .andExpect(status().isNoContent());
    }

    @Test
    void 인증되지_않은_이메일로는_회원가입할_수_없다() throws Exception {
        String email = uniqueEmail("unverified");

        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody(email)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));

        assertThat(userRepository.existsByEmailAndDeletedAtIsNull(email)).isFalse();
    }

    @Test
    void 인증된_이메일로_가입하면_인증완료_시각을_저장하고_Redis_상태를_지운다() throws Exception {
        String email = uniqueEmail("verified");
        when(verificationStore.isVerified(email)).thenReturn(true);

        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody(email)))
            .andExpect(status().isCreated());

        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(verificationStore).clearVerification(email);
    }

    private String signupBody(String email) {
        return """
            {
              "email":"%s",
              "nickname":"user-%s",
              "password":"password123"
            }
            """.formatted(email, UUID.randomUUID().toString().substring(0, 8));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
