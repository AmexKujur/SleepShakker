plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

android {
    namespace = "com.example.sleepshakker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sleepshakker"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    androidTestImplementation(libs.androidx.espresso.core)
    val room_version = "2.6.1" // Use the latest Room version

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version") // For Java projects
    ksp("androidx.room:room-compiler:$room_version") // For Kotlin projects using KSP

    // Optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:${room_version}")
}