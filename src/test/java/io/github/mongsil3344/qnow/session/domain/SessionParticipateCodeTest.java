package io.github.mongsil3344.qnow.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionParticipateCodeTest {

    @Test
    void 참가_코드를_발급하면_영문과_숫자_네_자리씩_하이픈으로_구분한다() {
        Session session = Session.builder()
            .organizationId(UUID.randomUUID())
            .creatorId(UUID.randomUUID())
            .title("session")
            .build();

        for (int attempt = 0; attempt < 100; attempt++) {
            SessionParticipateCode participateCode = SessionParticipateCode.create(session);

            assertThat(participateCode.getSession()).isSameAs(session);
            assertThat(participateCode.getCode())
                .matches("[A-HJ-KM-NP-Z2-9]{4}-[A-HJ-KM-NP-Z2-9]{4}");
        }
    }
}
