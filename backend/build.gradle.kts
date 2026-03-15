import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.2"
    id("com.github.spotbugs") version "6.0.27"
    id("net.ltgt.errorprone") version "4.1.0"
    checkstyle
    jacoco
}

group = "com.keplerops"
version = "0.19.0"

// -Pquick: disable slow static analysis for fast dev loops
val quick = providers.gradleProperty("quick").isPresent

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Audit trail
    implementation("org.springframework.data:spring-data-envers")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Logging (JSON output in production)
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // API docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // Error Prone
    errorprone("com.google.errorprone:error_prone_core:2.36.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // jqwik property-based testing
    testImplementation("net.jqwik:jqwik:1.9.2")

    // ArchUnit
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Instancio test data generation
    testImplementation("org.instancio:instancio-junit:5.2.1")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:1.21.1")
    testImplementation("org.testcontainers:junit-jupiter:1.21.1")
    testImplementation("org.testcontainers:postgresql:1.21.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.isEnabled.set(!quick)
    options.errorprone.disableWarningsInGeneratedCode = true
    options.errorprone.disable("MissingSummary")
}

if (quick) {
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach { enabled = false }
    tasks.named("checkstyleMain") { enabled = false }
}

tasks.register("rapid") {
    description = "Fast dev loop: format + compile (no tests, no static analysis)"
    group = "development"
    dependsOn("spotlessApply", "compileJava")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Unit tests only (fast, no DB)
tasks.test {
    useJUnitPlatform { excludeTags("integration", "age") }
    finalizedBy(tasks.jacocoTestReport)
}

// Integration tests (Testcontainers, slow)
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests with Testcontainers PostgreSQL"
    group = "verification"
    useJUnitPlatform { includeTags("integration"); excludeTags("age") }
    shouldRunAfter(tasks.test)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    finalizedBy(tasks.jacocoTestReport)
}

// AGE integration tests (requires Apache AGE Docker image)
tasks.register<Test>("ageTest") {
    description = "Runs Apache AGE integration tests"
    group = "verification"
    useJUnitPlatform { includeTags("age") }
    shouldRunAfter(tasks.test)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    // Merge coverage from integration tests when available
    val integrationTest = tasks.findByName("integrationTest")
    if (integrationTest != null) {
        executionData(integrationTest)
    }
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// SpotBugs
spotbugs {
    effort = com.github.spotbugs.snom.Effort.MAX
    excludeFilter = file("config/spotbugs/exclusions.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") { required = true }
    reports.create("xml") { required = false }
}

// Checkstyle
checkstyle {
    toolVersion = "10.21.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

// Exclude checkstyle on test sources — focus on production code
tasks.checkstyleTest {
    enabled = false
}

apply(from = "gradle/openjml.gradle.kts")

spotless {
    java {
        importOrder()
        removeUnusedImports()
        cleanthat()
        palantirJavaFormat()
        formatAnnotations()
    }
}
