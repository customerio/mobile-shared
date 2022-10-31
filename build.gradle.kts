buildscript {
    val sqlDelightVersion = "1.5.3"

    dependencies {
        classpath("com.squareup.sqldelight:gradle-plugin:$sqlDelightVersion")
    }
}

plugins {
    // trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("7.3.1").apply(false)
    kotlin("multiplatform").version("1.7.10").apply(false)
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply(from = "misc/lint.gradle")
