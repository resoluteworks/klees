plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "1.9.22"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jacoco:org.jacoco.core:0.8.10")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.0")
    implementation("com.github.nbaztec:coveralls-jacoco-gradle-plugin:1.2.18")
}
