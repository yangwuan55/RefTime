pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  
  resolutionStrategy {
    eachPlugin {
      if (requested.id.namespace == "org.jetbrains.compose") {
        useModule("org.jetbrains.compose:compose-gradle-plugin:${requested.version}")
      }
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
  }
}

rootProject.name = "TrueTime Kt"

include(":app")

include(":library")
include(":desktop-demo")
include(":ui")
