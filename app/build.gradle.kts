plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ofh.harness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ofh.harness"
        minSdk = 24
        targetSdk = 28
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val ksDefault = rootProject.file("release.keystore").absoluteFile
            val ksFile = File(System.getenv("OFH_KEYSTORE_FILE") ?: ksDefault.absolutePath)
            if (ksFile.isFile) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = ksFile
                    storePassword = System.getenv("OFH_KEYSTORE_PASS") ?: "ofhlauncher123"
                    keyAlias = "ofh"
                    keyPassword = System.getenv("OFH_KEYSTORE_PASS") ?: "ofhlauncher123"
                }
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint { disable += "ExpiredTargetSdkVersion" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
