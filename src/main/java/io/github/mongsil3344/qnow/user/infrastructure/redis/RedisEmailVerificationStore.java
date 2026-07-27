package io.github.mongsil3344.qnow.user.infrastructure.redis;

import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.UNAVAILABLE;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.github.mongsil3344.qnow.user.application.EmailVerificationStore;
import io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RedisEmailVerificationStore implements EmailVerificationStore {

    private static final String KEY_PREFIX = "qnow:email-verification:";

    private static final RedisScript<String> CREATE_REQUEST_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/email-verification/create-request.lua"),
        String.class
    );
    private static final RedisScript<Long> CANCEL_REQUEST_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/email-verification/cancel-request.lua"),
        Long.class
    );
    private static final RedisScript<String> VERIFY_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/email-verification/verify.lua"),
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Duration codeTtl;
    private final Duration verifiedTtl;
    private final Duration requestCooldown;
    private final Duration requestLimitWindow;
    private final int requestLimit;
    private final int maxAttempts;

    public RedisEmailVerificationStore(
        StringRedisTemplate redisTemplate,
        @Value("${qnow.email-verification.code-ttl}") Duration codeTtl,
        @Value("${qnow.email-verification.verified-ttl}") Duration verifiedTtl,
        @Value("${qnow.email-verification.request-cooldown}") Duration requestCooldown,
        @Value("${qnow.email-verification.request-limit-window}") Duration requestLimitWindow,
        @Value("${qnow.email-verification.request-limit}") int requestLimit,
        @Value("${qnow.email-verification.max-attempts}") int maxAttempts
    ) {
        this.redisTemplate = redisTemplate;
        this.codeTtl = codeTtl;
        this.verifiedTtl = verifiedTtl;
        this.requestCooldown = requestCooldown;
        this.requestLimitWindow = requestLimitWindow;
        this.requestLimit = requestLimit;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public RequestResult createRequest(String email, String code) {
        String prefix = keyPrefix(email);

        try {
            String result = redisTemplate.execute(
                CREATE_REQUEST_SCRIPT,
                List.of(
                    prefix + "code",
                    prefix + "attempts",
                    prefix + "cooldown",
                    prefix + "request-count",
                    prefix + "verified"
                ),
                code,
                Long.toString(codeTtl.toMillis()),
                Long.toString(requestCooldown.toMillis()),
                Long.toString(requestLimitWindow.toMillis()),
                Integer.toString(requestLimit)
            );
            if (!StringUtils.hasText(result)) {
                throw new IllegalStateException("Redis create-request script returned no result");
            }
            return RequestResult.valueOf(result);
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void cancelRequest(String email, String code) {
        String prefix = keyPrefix(email);

        try {
            Long result = redisTemplate.execute(
                CANCEL_REQUEST_SCRIPT,
                List.of(
                    prefix + "code",
                    prefix + "attempts",
                    prefix + "cooldown",
                    prefix + "request-count"
                ),
                code
            );
            if (result == null) {
                throw new IllegalStateException("Redis cancel-request script returned no result");
            }
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public VerificationResult verify(String email, String code) {
        String prefix = keyPrefix(email);

        try {
            String result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(
                    prefix + "code",
                    prefix + "attempts",
                    prefix + "verified",
                    prefix + "cooldown"
                ),
                code,
                Integer.toString(maxAttempts),
                Long.toString(verifiedTtl.toMillis())
            );
            if (!StringUtils.hasText(result)) {
                throw new IllegalStateException("Redis verify script returned no result");
            }
            return VerificationResult.valueOf(result);
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public boolean isVerified(String email) {
        try {
            Boolean result = redisTemplate.hasKey(keyPrefix(email) + "verified");
            if (result == null) {
                throw new IllegalStateException("Redis verification lookup returned no result");
            }
            return result;
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void clearVerification(String email) {
        try {
            Boolean result = redisTemplate.delete(keyPrefix(email) + "verified");
            if (result == null) {
                throw new IllegalStateException("Redis verification delete returned no result");
            }
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private String keyPrefix(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(email.getBytes(UTF_8));
            return KEY_PREFIX + "{" + HexFormat.of().formatHex(digest) + "}:";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private EmailVerificationException unavailable(RuntimeException cause) {
        return new EmailVerificationException(UNAVAILABLE, cause);
    }
}
