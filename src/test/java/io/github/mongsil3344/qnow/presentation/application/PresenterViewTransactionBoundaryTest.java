package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

class PresenterViewTransactionBoundaryTest {

    @Test
    void presenterViewOperationsUseWriteCapableTransactionsForSessionLifecycleLocks() throws Exception {
        assertWriteCapableTransaction(method(
            GetPresenterViewService.class,
            "getPresenterView",
            UUID.class,
            UUID.class,
            UUID.class
        ));
        assertWriteCapableTransaction(method(
            UpdatePresenterViewService.class,
            "updatePresenterView",
            UUID.class,
            UUID.class,
            UUID.class,
            UUID.class,
            int.class
        ));
    }

    private Method method(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return type.getDeclaredMethod(name, parameterTypes);
    }

    private void assertWriteCapableTransaction(Method method) {
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }
}
