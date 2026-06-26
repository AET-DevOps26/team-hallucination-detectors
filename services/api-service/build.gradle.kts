plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "de.tum.devops"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// Server interfaces and models generated from api/openapi.yaml — the contract
// is the single source of truth. Regenerate with api/scripts/gen-all.sh; never
// edit these sources or hand-write DTOs. CI fails on drift from the spec.
sourceSets {
    main {
        java {
            srcDir("src/generated/java")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Bean-validation annotations referenced by the generated models.
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Persistence: Flyway owns the schema (documented in src/main/resources/db/migration),
    // Hibernate only validates against it.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    // Tests run against in-memory H2 in PostgreSQL mode (same pattern as auth-service).
    testRuntimeOnly("com.h2database:h2")

    // JWT validation (tokens are issued by the auth-service with the same library).
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
