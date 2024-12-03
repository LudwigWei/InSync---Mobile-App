plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.insync_smartcontrolsystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.insync_smartcontrolsystem"
        minSdk = 24  // Change to 31 or higher
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation ("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1") // or latest version
    implementation("androidx.core:core:1.10.0") // or latest version
    implementation(libs.appcompat)  // If you have libs.versions.toml configured correctly
    implementation(libs.material)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)  // If you have libs.versions.toml configured correctly

    testImplementation(libs.junit)  // Make sure libs is configured
    androidTestImplementation(libs.ext.junit)  // Make sure libs is configured
    androidTestImplementation(libs.espresso.core)  // Make sure libs is configured
}
