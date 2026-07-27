package com.vantage;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.vantage.api.api.ApiApi;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.vantage")
public class ControllerApiContractTest {

    @ArchTest
    static final ArchRule controllers_should_implement_api_interface =
        classes()
            .that().areAnnotatedWith(RestController.class)
            .and().resideInAnyPackage("com.vantage..ui")
            .should().implement(ApiApi.class)
            .because("All REST controllers must implement the generated API interface to enforce contract");
}
