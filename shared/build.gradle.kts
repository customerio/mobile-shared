plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.squareup.sqldelight")
}

sqldelight {
    database("CioDatabase") {
        packageName = "com.customerio.shared.local"
    }
}

kotlin {
    android()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val sqlDelightVersion = "1.5.3"

        val commonMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:runtime:$sqlDelightVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:android-driver:$sqlDelightVersion")
            }
        }
        val androidTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:$sqlDelightVersion")
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.customerio.shared"
    compileSdk = 32
    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }
}