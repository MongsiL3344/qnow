package io.github.mongsil3344.qnow.user.infrastructure.redis;

import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.CREATED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.REQUEST_LIMIT_EXCEEDED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.TOO_FREQUENT;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.ALREADY_VERIFIED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.ATTEMPTS_EXCEEDED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.CODE_MISMATCH;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisEmailVerificationStoreTest {

    private static final int REDIS_PORT = 6379;
    private static final Duration COOLDOWN = Duration.ofMillis(50);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisEmailVerificationStore store;

    @BeforeAll
    static void setUpRedis() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
            REDIS.getHost(),
            REDIS.getMappedPort(REDIS_PORT)
        );
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        store = new RedisEmailVerificationStore(
            redisTemplate,
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            COOLDOWN,
            Duration.ofHours(1),
            2,
            3
        );
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void cooldown_중에는_인증번호를_반복_요청할_수_없다() {
        String email = uniqueEmail();

        assertThat(store.createRequest(email, "123456")).isEqualTo(CREATED);
        assertThat(store.createRequest(email, "654321")).isEqualTo(TOO_FREQUENT);
    }

    @Test
    void 발급_횟수는_설정된_시간창_안에서_제한된다() throws Exception {
        String email = uniqueEmail();

        assertThat(store.createRequest(email, "123456")).isEqualTo(CREATED);
        Thread.sleep(COOLDOWN.plusMillis(50));
        assertThat(store.createRequest(email, "234567")).isEqualTo(CREATED);
        Thread.sleep(COOLDOWN.plusMillis(50));
        assertThat(store.createRequest(email, "345678")).isEqualTo(REQUEST_LIMIT_EXCEEDED);
    }

    @Test
    void 틀린_인증번호를_세_번_입력하면_더_이상_확인할_수_없다() {
        String email = uniqueEmail();
        store.createRequest(email, "123456");

        assertThat(store.verify(email, "000000")).isEqualTo(CODE_MISMATCH);
        assertThat(store.verify(email, "111111")).isEqualTo(CODE_MISMATCH);
        assertThat(store.verify(email, "222222")).isEqualTo(ATTEMPTS_EXCEEDED);
        assertThat(store.verify(email, "123456")).isEqualTo(ATTEMPTS_EXCEEDED);
    }

    @Test
    void 정확한_인증번호는_인증완료_상태를_만들고_확인은_멱등적이다() {
        String email = uniqueEmail();
        store.createRequest(email, "123456");

        assertThat(store.verify(email, "123456")).isEqualTo(VERIFIED);
        assertThat(store.isVerified(email)).isTrue();
        assertThat(store.verify(email, "123456")).isEqualTo(ALREADY_VERIFIED);

        store.clearVerification(email);
        assertThat(store.isVerified(email)).isFalse();
    }

    @Test
    void 전송_실패로_요청을_취소하면_즉시_다시_발급할_수_있다() {
        String email = uniqueEmail();
        store.createRequest(email, "123456");

        store.cancelRequest(email, "123456");

        assertThat(store.createRequest(email, "654321")).isEqualTo(CREATED);
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
