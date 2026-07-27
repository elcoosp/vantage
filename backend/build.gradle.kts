plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "5.1.0.4882"
    id("jacoco")
    id("org.openapi.generator") version "7.5.0"
}

group = "com.vantage"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:context-propagation")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:rabbitmq")

    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("net.jqwik:jqwik:1.8.2")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.3.0")
        mavenBom("org.testcontainers:testcontainers-bom:2.0.5")
        mavenBom("io.github.resilience4j:resilience4j-bom:2.2.0")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}


tasks.withType<Test> {
    useJUnitPlatform()
    // Exclude integration tests (they require Docker and external services)
    exclude("**/*IT.class")
    exclude("**/*IntegrationTest.class")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

sonar {
    properties {
        property("sonar.projectKey", "vantage-backend")
        property("sonar.organization", "vantage-org")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get().asFile}/reports/jacoco/test/jacocoTestReport.xml")
    }
}

sourceSets {
    main {
        java {
            srcDir("$rootDir/src/generated/src/main/java")
        }
    }
}

val generateOpenApiModels by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("spring")
    inputSpec.set("$rootDir/../docs/02-contracts/02-rest-api-spec.yaml")
    outputDir.set("$rootDir/src/generated")
    apiPackage.set("com.vantage.api.api")
    modelPackage.set("com.vantage.api.model")
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "interfaceOnly" to "true",
        "useJakartaEe" to "true",
        "dateLibrary" to "java8",
        "serializableModel" to "true"
    ))
}

tasks.compileJava {
    dependsOn(generateOpenApiModels)
}