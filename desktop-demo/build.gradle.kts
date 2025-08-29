plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0"
}

kotlin {
    jvmToolchain(20)
}

// Use default Java toolchain

dependencies {
    implementation(project(":library"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.bom)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}

compose.desktop {
    application {
        mainClass = "com.milo.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "TrueTime Desktop Demo"
            packageVersion = "1.0.0"
        }
    }
}