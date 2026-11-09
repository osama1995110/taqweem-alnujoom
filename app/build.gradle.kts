plugins {
    id("com.android.application")
}

android {
    namespace = "com.nujoom.calendar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nujoom.calendar"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
