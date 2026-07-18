package io.github.mongsil3344.qnow.organization.infrastructure.repo;

import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {
    boolean existsByUserIdAndOrganizationIdAndDeletedAtIsNull(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationIdAndRoleAndDeletedAtIsNull(UUID userId, UUID organizationId, UserGroupRole role);

    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    Slice<UserGroup> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    @Query("""
        select ug
        from UserGroup ug
        join fetch ug.organization organization
        where ug.userId = :userId
            and ug.deletedAt is null
            and organization.deletedAt is null
        order by ug.createdAt desc
        """)
    List<UserGroup> findAllActiveByUserIdWithOrganization(@Param("userId") UUID userId);

    @Query("""
        select ug.organization.id as organizationId, count(ug.id) as memberCount
        from UserGroup ug
        where ug.organization.id in :organizationIds
            and ug.deletedAt is null
        group by ug.organization.id
        """)
    List<OrganizationMemberCount> countMembersByOrganizationIds(
        @Param("organizationIds") Collection<UUID> organizationIds
    );

    @Query("""
        select ug.organization.id
        from UserGroup ug
        where ug.userId = :userId
            and ug.organization.id in :organizationIds
            and ug.deletedAt is null
        """)
    Set<UUID> findActiveOrganizationIdsByUserIdAndOrganizationIds(
        @Param("userId") UUID userId,
        @Param("organizationIds") Collection<UUID> organizationIds
    );

    interface OrganizationMemberCount {
        UUID getOrganizationId();

        long getMemberCount();
    }
}
