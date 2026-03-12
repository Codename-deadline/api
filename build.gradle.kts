import com.google.protobuf.gradle.id

plugins {
	kotlin("jvm") version "2.3.10"
	kotlin("plugin.spring") version "2.3.10"
    kotlin("plugin.jpa") version "2.3.10"
	id("org.springframework.boot") version "4.0.3"
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

extra["springGrpcVersion"] = "1.0.2"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    implementation("org.springframework.kafka:spring-kafka") {
        exclude(module = "lz4-java")
    }
    // Original package is archived. Community maintained fork.
    implementation("at.yawk.lz4:lz4-java:1.10.4")

    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    implementation("io.github.sanvew:telegram-init-data:1.0.0")

    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.2")
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("redis.clients:jedis")
	implementation("io.minio:minio:8.6.0")
	implementation("org.apache.tika:tika-core:3.2.3")
    implementation("org.apache.tika:tika-parsers-standard-package:3.2.3")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
    testImplementation("com.h2database:h2:2.4.240")
	testImplementation("io.mockk:mockk:1.14.9")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.grpc:spring-grpc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.4"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.79.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
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
