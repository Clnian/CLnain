// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加阿里云 Maven 仓库镜像，用于加速依赖下载
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 添加 JitPack 仓库，MPAndroidChart 库需要从这里获取
        maven { url = uri("https://jitpack.io") }
        // SQLScout 的仓库已移除，因为它已失效
    }
}

rootProject.name = "CLnain"
include(":app")