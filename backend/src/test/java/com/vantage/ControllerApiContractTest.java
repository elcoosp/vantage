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
            .and().resideInAnyPackage(
                "com.vantage.product.ui",
                "com.vantage.order.ui",
                "com.vantage.inventory.ui",
                "com.vantage.payment.ui",
                "com.vantage.analytics.ui",
                "com.vantage.vendor.ui"
            )
            .should().implement(ApiApi.class)
            .because("Controllers for endpoints defined in the OpenAPI spec must implement the generated API interface");
}
