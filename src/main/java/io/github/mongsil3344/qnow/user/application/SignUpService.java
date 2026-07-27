package io.github.mongsil3344.qnow.user.application;

import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateNicknameException;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.time.Instant;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SignUpService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signUp(String email, String nickname, String password) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);

        boolean existEmail = userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail);

        if (existEmail) {
            throw new DuplicateEmailException();
        }

        boolean existNickname = userRepository.existsByNicknameAndDeletedAtIsNull(nickname);

        if (existNickname) {
            throw new DuplicateNicknameException();
        }

        emailVerificationService.requireVerified(normalizedEmail);

        String passwordHashed = passwordEncoder.encode(password);

        User user = User.builder()
            .email(normalizedEmail)
            .nickname(nickname)
            .password(passwordHashed)
            .emailVerifiedAt(Instant.now())
            .build();

        userRepository.saveAndFlush(user);
        emailVerificationService.clearVerification(normalizedEmail);
    }
}
