package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.application.dto.OrganizationMemberSliceResult;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationMemberSliceResult.MemberResult;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationMemberSliceResult.Role;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationMemberListQueryException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationMemberRequiredException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationNotFoundException;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetOrganizationMemberListService {

    private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";

    private final OrganizationRepository organizationRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public OrganizationMemberSliceResult getOrganizationMembers(
        UUID organizationId,
        UUID requesterId,
        int page,
        int size
    ) {
        validatePagination(page, size);
        validateAccess(organizationId, requesterId);

        Slice<UserGroup> memberSlice = userGroupRepository.findAllByOrganizationIdAndDeletedAtIsNull(
            organizationId,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "role")
                    .and(Sort.by(Sort.Direction.ASC, "createdAt"))
                    .and(Sort.by(Sort.Direction.ASC, "id"))
            )
        );
        List<UserGroup> members = memberSlice.getContent();
        Map<UUID, String> nicknames = userQueryApi.findNicknamesByIds(
            members.stream()
                .map(UserGroup::getUserId)
                .toList()
        );
        List<MemberResult> content = members.stream()
            .map(member -> new MemberResult(
                nicknames.getOrDefault(member.getUserId(), UNKNOWN_USER_NAME),
                toRole(member.getRole()),
                member.getCreatedAt()
            ))
            .toList();

        return new OrganizationMemberSliceResult(
            content,
            memberSlice.getNumber(),
            memberSlice.getSize(),
            memberSlice.hasNext()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidOrganizationMemberListQueryException();
        }
    }

    private void validateAccess(UUID organizationId, UUID requesterId) {
        if (organizationRepository.findByIdAndDeletedAtIsNull(organizationId).isEmpty()) {
            throw new OrganizationNotFoundException();
        }

        if (!userGroupRepository.existsByUserIdAndOrganizationIdAndDeletedAtIsNull(requesterId, organizationId)) {
            throw new OrganizationMemberRequiredException();
        }
    }

    private Role toRole(UserGroupRole role) {
        return role == UserGroupRole.ADMIN ? Role.ADMIN : Role.MEMBER;
    }
}
