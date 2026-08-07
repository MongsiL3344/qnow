package io.github.mongsil3344.qnow.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            CsrfTokenRequestHandler csrfTokenRequestHandler
    ) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        .ignoringRequestMatchers("/login", "/signup", "/email-verifications/**")
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/csrf",
                                "/signup",
                                "/login",
                                "/email-verifications/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/modulith",
                                "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers("/guest/session-participations").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/organizations/*/sessions/*",
                                "/organizations/*/sessions/*/presentations",
                                "/organizations/*/sessions/*/presentations/*/pdf",
                                "/organizations/*/sessions/*/presenter-view",
                                "/presentations/*/questions"
                        ).hasAnyRole("MEMBER", "GUEST")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/organizations/*/sessions/*/participants/exit",
                                "/presentations/*/questions",
                                "/presentations/*/questions/control-requests"
                        ).hasAnyRole("MEMBER", "GUEST")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/questions/*/upvote",
                                "/organizations/*/sessions/*/presenter-view"
                        ).hasAnyRole("MEMBER", "GUEST")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/questions/*/upvote",
                                "/questions/*",
                                "/organizations/*/sessions/*/presenter-view/controller"
                        ).hasAnyRole("MEMBER", "GUEST")
                        .requestMatchers("/ws").hasAnyRole("MEMBER", "GUEST")
                        .requestMatchers("/organizations/**", "/users/**").hasRole("MEMBER")
                        .anyRequest().hasRole("MEMBER")
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)
                        )
                )
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new CsrfTokenRequestAttributeHandler();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
