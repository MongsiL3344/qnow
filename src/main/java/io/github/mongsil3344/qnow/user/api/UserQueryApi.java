package io.github.mongsil3344.qnow.user.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserQueryApi {
    boolean existsUser(UUID userID);

    Map<UUID, String> findNicknamesByIds(Collection<UUID> userIds);
}
