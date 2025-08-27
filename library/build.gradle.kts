// remove this on upgrading android gradle plugin to 8
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  id("maven-publish")
}

android {
  namespace = "com.milo.reftime"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    aarMetadata { minCompileSdk = libs.versions.compileSdk.get().toInt() }
  }

  buildTypes { getByName("release") { isMinifyEnabled = false } }

  buildFeatures { buildConfig = false }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.datetime)
  api(libs.ktor.client.core)
  api(libs.ktor.client.okhttp)
  api(libs.ktor.network)
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        groupId = "com.instacart"
        artifactId = "truetime"
        version = libs.versions.trueTime.get()
        afterEvaluate { from(components["release"]) }
      }
    }
  }
}
