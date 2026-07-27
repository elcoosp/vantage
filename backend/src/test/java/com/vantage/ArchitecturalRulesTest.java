package com.vantage;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.conditions.ArchConditions.*;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.*;

@AnalyzeClasses(packages = "com.vantage")
public class ArchitecturalRulesTest {

    @ArchTest
    static final ArchRule layeredArchitectureRule = layeredArchitecture()
            .layer("UI").definedBy("..ui..")
            .layer("App").definedBy("..app..")
            .layer("Domain").definedBy("..domain..")
            .layer("Messaging").definedBy("..messaging..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("UI").mayOnlyBeAccessedByLayers("App")
            .whereLayer("App").mayOnlyAccessLayers("UI", "Domain", "Messaging")
            .whereLayer("Domain").mayOnlyAccessLayers("Domain")
            .whereLayer("Messaging").mayOnlyAccessLayers("App", "Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "App");

    @ArchTest
    static final ArchRule controllerIsolationRule =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().accessClassesThat().areAnnotatedWith(Repository.class)
                    .orShould().accessClassesThat().areAssignableTo(JpaRepository.class);

    @ArchTest
    static final ArchRule dtoBoundaryRule =
            noMethods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .should().haveRawReturnType(anyClassThat().isAnnotatedWith(Entity.class));

    @ArchTest
    static final ArchRule serviceIsolationRule =
            noMethods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .should().callMethodWhere(
                            target(owner(anyClassThat().isAnnotatedWith(Service.class)))
                            .and(target(returnType(anyClassThat().isAnnotatedWith(Entity.class))))
                    );

    @ArchTest
    static final ArchRule noFieldInjectionRule =
            noFields()
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);
}
