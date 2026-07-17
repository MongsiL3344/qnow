package io.github.mongsil3344.qnow.user.application;

import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateNicknameException;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SignUpService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(String email, String nickname, String password) {

        boolean existEmail = userRepository.existsByEmailAndDeletedAtIsNull(email);

        if (existEmail) {
            throw new DuplicateEmailException();
        }

        boolean existNickname = userRepository.existsByNicknameAndDeletedAtIsNull(nickname);

        if (existNickname) {
            throw new DuplicateNicknameException();
        }

        String passwordHashed = passwordEncoder.encode(password);

        User user = User.builder()
            .email(email)
            .nickname(nickname)
            .password(passwordHashed)
            .build();

        userRepository.save(user);
    }
}
