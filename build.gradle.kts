import com.google.protobuf.gradle.id

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.6"
}

group = "xyz.om3lette"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-grpc-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    implementation("org.springframework.kafka:spring-kafka") {
        exclude(module = "lz4-java")
    }
    // Original package is archived. Community maintained fork.
    implementation("at.yawk.lz4:lz4-java:1.11.0")

    implementation("io.github.sanvew:telegram-init-data:1.0.0")

    implementation("io.hypersistence:hypersistence-utils-hibernate-73:3.15.3")
    implementation("tools.jackson.module:jackson-module-kotlin:3.2.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("redis.clients:jedis")
	implementation("io.minio:minio:9.0.2")
	implementation("software.amazon.awssdk:bom:2.46.17")
	implementation("org.apache.tika:tika-core:3.3.1")
    implementation("org.apache.tika:tika-parsers-standard-package:3.3.1")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
    testImplementation("org.springframework.boot:spring-boot-starter-grpc-server-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2:2.4.240")
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("SKIPPED", "FAILED")
	}
}
