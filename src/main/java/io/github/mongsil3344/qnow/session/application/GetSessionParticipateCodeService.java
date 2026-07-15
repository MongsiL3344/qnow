package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.application.dto.SessionParticipateCodeResult;
import io.github.mongsil3344.qnow.session.application.exception.OrganizationAdminRequiredException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.application.exception.SessionParticipateCodeNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.domain.SessionParticipateCode;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionParticipateCodeRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetSessionParticipateCodeService {

    private final SessionRepository sessionRepository;
    private final SessionParticipateCodeRepository participateCodeRepository;
    private final OrganizationQueryApi organizationQueryApi;

    @Transactional(readOnly = true)
    public SessionParticipateCodeResult getParticipateCode(
        UUID organizationId,
        UUID sessionId,
        UUID userId
    ) {
        Session session = sessionRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(sessionId, organizationId)
            .orElseThrow(SessionNotFoundException::new);

        if (!organizationQueryApi.isAdminInOrganization(userId, organizationId)) {
            throw new OrganizationAdminRequiredException();
        }

        SessionParticipateCode participateCode = participateCodeRepository.findActiveBySessionId(session.getId())
            .orElseThrow(SessionParticipateCodeNotFoundException::new);

        return new SessionParticipateCodeResult(session.getId(), participateCode.getCode());
    }
}
