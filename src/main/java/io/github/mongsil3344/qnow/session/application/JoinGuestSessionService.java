package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.application.dto.JoinGuestSessionResult;
import io.github.mongsil3344.qnow.session.application.exception.SessionParticipateCodeNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.domain.SessionParticipateCode;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionParticipateCodeRepository;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class JoinGuestSessionService {

    private final SessionParticipateCodeRepository participateCodeRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public JoinGuestSessionResult join(String code, String nickname) {
        SessionParticipateCode participateCode = participateCodeRepository
            .findActiveByCodeForJoin(normalizeCode(code))
            .orElseThrow(SessionParticipateCodeNotFoundException::new);

        Session session = participateCode.getSession();
        if (session.isEnded()) {
            throw new SessionEndedException();
        }

        Participant participant = participantRepository.save(
            Participant.guest(nickname.strip(), session)
        );

        return new JoinGuestSessionResult(
            participant.getId(),
            session.getId(),
            session.getOrganizationId(),
            participant.getGuestNickname()
        );
    }

    private String normalizeCode(String code) {
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() == 8) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4);
        }
        return normalized;
    }
}
