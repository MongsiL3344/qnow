package io.github.mongsil3344.qnow.user.infrastructure.web.dto;

import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String email,
    String nickname
) {}
