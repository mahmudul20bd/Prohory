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
        // 🔥 এই লাইনটি যোগ করুন (Nightly Builds এর জন্য)
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") } // এই সার্ভারটি মিসিং ছিল
        maven { url = uri("https://jitpack.io") } // <--- এই লাইনটি অবশ্যই যোগ করুন
    }
}

rootProject.name = "CyberAware"
include(":app")