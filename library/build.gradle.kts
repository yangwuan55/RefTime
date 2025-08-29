// Kotlin Multiplatform 配置
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  kotlin("multiplatform")
  id("maven-publish")
}

kotlin {
  // JVM 平台
  jvm {
    withJava()
    compilations.all {
      kotlinOptions {
        jvmTarget = "17"
      }
    }
  }

  // 共享源代码集配置
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.ktor.client.core)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(libs.ktor.client.cio)
      }
    }
  }
}

afterEvaluate {
  publishing {
    publications {
      // 多平台发布配置
      withType<MavenPublication> {
        groupId = "com.instacart"
        version = libs.versions.trueTime.get()
        artifactId = "truetime"
      }
    }
  }
}
