package io.github.mongsil3344.qnow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle;
import org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships;

class DocumentationTests {

    @Test
    void writesModulithDocumentation() {
        ApplicationModules modules = ApplicationModules.of(QnowApplication.class);

        new Documenter(modules)
                .writeDocumentation(
                        DiagramOptions.defaults()
                                .withStyle(DiagramStyle.UML)
                                .withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE),
                        CanvasOptions.defaults()
                );
    }
}
