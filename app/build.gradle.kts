@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.milo.sample"

  compileSdk = 34

  defaultConfig {
    applicationId = "com.instacart.truetime"
    minSdk = 21
    targetSdk = 34

    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.3"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  implementation(project(":library"))

  // Compose BOM
  implementation(platform("androidx.compose:compose-bom:2023.10.01"))

  // Compose Core
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")

  // Activity Compose
  implementation("androidx.activity:activity-compose:1.7.2")

  // Lifecycle
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

  // AndroidX Core
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

  // Debug tools
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
