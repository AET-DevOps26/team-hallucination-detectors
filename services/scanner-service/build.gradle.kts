plugins {
    java
    id("org.springframework.boot") version "3.4.13"
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

// Server interfaces and models generated from api/scanner-internal.yaml — the
// contract is the single source of truth. Regenerate with api/scripts/gen-all.sh;
// never edit these sources or hand-write DTOs. CI fails on drift from the spec.
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

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Spring Boot 3.4.13's managed versions (tomcat 10.1.50, jackson-bom 2.18.5)
// still lag behind known CVE fixes — override via the BOM's own version
// properties until a Spring Boot release picks up the patched versions.
extra["tomcat.version"] = "10.1.57"
extra["jackson-bom.version"] = "2.18.9"
