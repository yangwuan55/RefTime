@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  id("com.diffplug.spotless") version "6.20.0"
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**/*.kt")
    ktfmt("0.43")
  }

  format("kts") {
    target("**/*.gradle.kts")
    targetExclude("**/build/**/*.kts")
  }
}
