package io.github.mongsil3344.qnow.organization.infrastructure.repo;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.OrganizationStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsByNameAndStatusNot(String name, OrganizationStatus status);
}
