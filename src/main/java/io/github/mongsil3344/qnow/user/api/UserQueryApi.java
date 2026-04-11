package io.github.mongsil3344.qnow.user.api;

import java.util.UUID;

public interface UserQueryApi {
    boolean existsUser(UUID userID);
}
