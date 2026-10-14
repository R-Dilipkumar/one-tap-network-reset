plugins {
  id("com.android.application")
}

android {
  namespace = "com.rdilip.netonetap"
  // HiddenApiBypass 6.2 needs compileSdk 37. targetSdk stays 36.
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.rdilip.netonetap"
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"
  }

  buildTypes {
    release {
      // RootMain is started by class name through app_process, so keep names intact.
      isMinifyEnabled = false
    }
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("dev.rikka.shizuku:api:13.1.5")
  implementation("dev.rikka.shizuku:provider:13.1.5")
  implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.2")
}
