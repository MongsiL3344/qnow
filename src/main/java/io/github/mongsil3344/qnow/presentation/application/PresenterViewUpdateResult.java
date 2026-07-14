package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;

public record PresenterViewUpdateResult(PresenterViewSnapshot snapshot, boolean changed) {
}
