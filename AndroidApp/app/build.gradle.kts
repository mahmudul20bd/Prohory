plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cyberaware.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cyberaware.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        // 🔥 ২.১৬ ভার্সনের জন্য এই প্যাকেজিং অপশনগুলো খুব জরুরি, নাহলে ক্রাশ করবে
        packaging {
            jniLibs {
                pickFirst("lib/**/libc++_shared.so")
            }
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 🔥 Google ML Kit - Text Recognition (OCR)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // 🔥 CORE LIBRARIES
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(libs.remote.creation.core)


    // 🔥 JETPACK COMPOSE (BOM - BILL OF MATERIALS)
    // এটি সব ভার্সন কন্ট্রোল করবে, তাই নিচে ভার্সন নাম্বার দেয়ার দরকার নেই
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose Libraries (No Version Needed)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Icons
    implementation("androidx.compose.animation:animation") // Animations

    // 🔥 THIRD PARTY LIBRARIES (Versions Required)
    implementation("io.coil-kt:coil-compose:2.6.0") // Image Loading
    implementation("com.airbnb.android:lottie-compose:6.4.0") // Lottie Animation

    // 🔥 AI & ML (LiteRT - The New TFLite)
    // এটি ২.১৬.১ এর চেয়েও নতুন এবং v12 অপ সাপোর্ট করে
    val litertVersion = "1.4.1"

    implementation("com.google.ai.edge.litert:litert:$litertVersion")
    implementation("com.google.ai.edge.litert:litert-api:$litertVersion")

    // যদি ফ্লেক্স ডেলিগেট লাগে (আপনার মডেলের জন্য লাগতে পারে)
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")

    // 🔥 NETWORKING
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 🔥 TESTING
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Retrofit & Gson for API Calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Coroutines for background threading
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 🔥 ViewModel for Compose (এই লাইনটি যোগ করুন)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    // STOMP WebSocket & RxJava for Real-time Graph
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
}

configurations.all {
    resolutionStrategy {
        force("com.google.ai.edge.litert:litert:1.4.1")
        force("com.google.ai.edge.litert:litert-api:1.4.1")
        force("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    }
}