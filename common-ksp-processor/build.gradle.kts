plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":common-ksp-annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(project(":common-ksp-annotation"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
