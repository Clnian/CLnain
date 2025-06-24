// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    // 如果使用 Kotlin，请应用 Kotlin 插件:
    // alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.clnain"
    compileSdk = 35 // 与实验文档和现代实践保持一致

    defaultConfig {
        applicationId = "com.example.clnain"
        minSdk = 21      // 与实验文档和现代实践保持一致
        targetSdk = 35   // 与实验文档和现代实践保持一致
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 根据你的设置，发布时可考虑设为 true
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
    // 如果使用 Kotlin:
    // kotlinOptions {
    //    jvmTarget = "11"
    // }
}

dependencies {
    // AndroidX 核心库 (使用 libs.versions.toml 中定义的别名)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // implementation(libs.gson) // 你已添加的 Gson

    // 本地 NLE SDK (物联网云平台) - 确保 nlecloudII.jar 文件在 app/libs 目录下
    implementation(files("libs/nlecloudII.jar"))

    // MPAndroidChart 图表库
    implementation(libs.mpandroidchart)

    // SQLScout 依赖已移除

    // 测试库
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit) // androidx.test.ext:junit
    androidTestImplementation(libs.espresso.core)
}