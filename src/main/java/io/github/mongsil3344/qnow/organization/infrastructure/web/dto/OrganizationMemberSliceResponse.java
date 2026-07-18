package io.github.mongsil3344.qnow.organization.infrastructure.web.dto;

import io.github.mongsil3344.qnow.organization.application.dto.OrganizationMemberSliceResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record OrganizationMemberSliceResponse(
    List<MemberResponse> content,
    int page,
    int size,
    boolean hasNext
) {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    public static OrganizationMemberSliceResponse from(OrganizationMemberSliceResult result) {
        return new OrganizationMemberSliceResponse(
            result.content().stream()
                .map(MemberResponse::from)
                .toList(),
            result.page(),
            result.size(),
            result.hasNext()
        );
    }

    public record MemberResponse(
        String nickname,
        OrganizationMemberSliceResult.Role role,
        LocalDate joinedAt
    ) {

        private static MemberResponse from(OrganizationMemberSliceResult.MemberResult result) {
            return new MemberResponse(
                result.nickname(),
                result.role(),
                LocalDate.ofInstant(result.joinedAt(), SERVICE_ZONE_ID)
            );
        }
    }
}
