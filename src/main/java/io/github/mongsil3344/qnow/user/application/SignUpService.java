package io.github.mongsil3344.qnow.user.application;

import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateUsernameException;
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
    public void signUp(String email, String nickname, String username, String password) {

        boolean existEmail = userRepository.existsByEmailAndDeletedAtIsNull(email);
        boolean existUsername = userRepository.existsByUsernameAndDeletedAtIsNull(username);

        if (existEmail) {
            throw new DuplicateEmailException();
        }

        if (existUsername) {
            throw new DuplicateUsernameException();
        }

        String passwordHashed = passwordEncoder.encode(password);

        User user = User.builder()
            .email(email)
            .nickname(nickname)
            .username(username)
            .password(passwordHashed)
            .build();

        userRepository.save(user);
    }
}
