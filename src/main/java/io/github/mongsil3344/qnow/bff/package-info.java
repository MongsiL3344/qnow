@ApplicationModule(
        displayName = "BFF",
        allowedDependencies = {"organization::api", "session::api", "user::api"}
)
package io.github.mongsil3344.qnow.bff;

import org.springframework.modulith.ApplicationModule;
