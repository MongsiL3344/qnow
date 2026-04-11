package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.application.exception.DuplicateNameException;
import io.github.mongsil3344.qnow.organization.application.exception.UserNotFoundException;
import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.OrganizationStatus;
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

@Service
@AllArgsConstructor
public class CreateOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserQueryApi userQueryApi;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createOrganization(UUID userId, String name, String detail, String password) {

        if (!userQueryApi.existsUser(userId)) {
            throw new UserNotFoundException();
        }

        boolean existName = organizationRepository.existsByNameAndStatusNot(name, OrganizationStatus.DELETED);
        if (existName) {
            throw new DuplicateNameException();
        }

        String passwordHashed = null;

        if (password != null) {
            passwordHashed = passwordEncoder.encode(password);
        }

        Organization newOrg = Organization.builder()
            .name(name)
            .detail(detail)
            .password(passwordHashed)
            .build();

        organizationRepository.save(newOrg);

        UserGroup newUserGroup = UserGroup.builder()
            .userId(userId)
            .organization(newOrg)
            .role(UserGroupRole.ADMIN)
            .build();

        userGroupRepository.save(newUserGroup);
    }
}
