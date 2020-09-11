plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.spring") version Versions.kotlin
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.test {
    useJUnitPlatform {}
}

dependencies {
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")

    implementation("org.springframework.boot:spring-boot-starter:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${Versions.springBoot}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinCoroutines}")
    implementation("com.google.guava:guava:${Versions.guava}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
}

application {
    mainClassName = "mi.lab.ApplicationKt"
}
