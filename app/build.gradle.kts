plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.smstotgsender"
    // Рекомендую использовать стабильную версию SDK (34 или 35),
    // 36 - это превью версия, она может вызывать ошибки на некоторых устройствах.
    // Но если вам нужна именно 36, оставьте как было. Я поставил 34 для стабильности.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smstotgsender"
        minSdk = 24
        targetSdk = 34 // Также рекомендую стабильную версию
        versionCode = 3 // Увеличил версию для нового билда
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // !!! ГЛАВНЫЕ ИЗМЕНЕНИЯ !!!
            isMinifyEnabled = true       // Включаем обфускацию
            isShrinkResources = true     // Включаем удаление лишнего

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
    androidTestImplementation(libs.androidx.espresso.core)
}