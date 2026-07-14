package io.github.mongsil3344.qnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void 모듈_구조를_검증한다() {
        ApplicationModules.of(QnowApplication.class).verify();
    }
}
