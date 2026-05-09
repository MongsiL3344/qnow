package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.application.exception.AlreadyOrganizationMemberException;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationPasswordException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationNotFoundException;
import io.github.mongsil3344.qnow.organization.application.exception.UserNotFoundException;
import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class JoinOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserQueryApi userQueryApi;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void joinOrganization(UUID organizationId, UUID userId, String password) {
        // 만약 유저가 존재하지 않으면 예외발생
        if (!userQueryApi.existsUser(userId)) {
            throw new UserNotFoundException();
        }

        // 조직 ID로 조직이 있는지 찾고 없는 조직이면 예외 발생
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        // 이미 조직에 참여한 유저면 예외 발생
        boolean alreadyMember = userGroupRepository.existsByUserIdAndOrganizationIdAndDeletedAtIsNull(
            userId,
            organizationId
        );
        if (alreadyMember) {
            throw new AlreadyOrganizationMemberException();
        }

        // 조직이 비밀번호가 필요한데 비밀번호가 올바르지 않을 경우 예외 발생
        if (requiresPassword(organization) && !matchesPassword(password, organization.getPassword())) {
            throw new InvalidOrganizationPasswordException();
        }

        UserGroup userGroup = UserGroup.builder()
            .userId(userId)
            .organization(organization)
            .role(UserGroupRole.USER)
            .build();

        userGroupRepository.save(userGroup);
    }

    // 참여하고자 하는 조직이 비밀번호를 설정했는지 확인하는 메서드
    private boolean requiresPassword(Organization organization) {
        return organization.getPassword() != null;
    }

    // 비밀번호 검사
    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
