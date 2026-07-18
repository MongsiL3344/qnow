package io.github.mongsil3344.qnow.organization.application.dto;

import java.time.Instant;
import java.util.List;

public record OrganizationMemberSliceResult(
    List<MemberResult> content,
    int page,
    int size,
    boolean hasNext
) {

    public record MemberResult(
        String nickname,
        Role role,
        Instant joinedAt
    ) {
    }

    public enum Role {
        ADMIN,
        MEMBER
    }
}
