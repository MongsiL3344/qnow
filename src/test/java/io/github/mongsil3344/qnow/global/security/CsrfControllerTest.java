package io.github.mongsil3344.qnow.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Test
    void csrfTokenCanBeIssuedWithoutAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").doesNotExist())
                .andExpect(jsonPath("$.parameterName").doesNotExist())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void csrfTokenIsReusedWithinSameSession() throws Exception {
        MvcResult firstResult = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) firstResult.getRequest().getSession(false);
        String firstToken = readToken(firstResult);

        mockMvc.perform(get("/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(firstToken));
    }

    @Test
    void signupAndLoginDoNotRequireCsrfToken() throws Exception {
        String email = "csrf-exempt-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "nickname": "csrf-test",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk());
    }

    @Test
    void issuedCsrfTokenAuthorizesProtectedMutation() throws Exception {
        String email = "csrf-protected-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        signup(email, password);
        MvcResult loginResult = login(email, password);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        MvcResult csrfResult = mockMvc.perform(get("/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = readToken(csrfResult);

        mockMvc.perform(post("/logout")
                .session(session)
                .header("X-CSRF-TOKEN", csrfToken))
            .andExpect(status().isOk());
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "nickname": "csrf-test",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isCreated());
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    }

    private String readToken(MvcResult result) {
        CsrfToken csrfToken = csrfTokenRepository.loadToken(result.getRequest());

        assertThat(csrfToken).isNotNull();
        return csrfToken.getToken();
    }
}
