package com.vantage;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;

@AnalyzeClasses(packages = "com.vantage")
public class ArchitecturalRulesTest {

    @ArchTest
    static final ArchRule dtoBoundaryRule =
            noMethods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .should().haveRawReturnType(annotatedWith(Entity.class));
}
