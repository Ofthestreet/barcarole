plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The upload key, when this build has one: an explicit path, or the file CI writes from the
// repository secrets. Never committed - see .gitignore.
val uploadKeystore: String? = System.getenv("BARCAROLE_KEYSTORE_FILE")
    ?: rootProject.file("keystore/upload.jks").takeIf { it.exists() }?.absolutePath

android {
    namespace = "io.github.ofthestreet.barcarole"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ofthestreet.barcarole"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // A stable signing key is what lets a new version install over the previous one. When the
    // build has none - a clone without the secrets - it falls back to the throwaway debug key,
    // which is regenerated per machine and therefore breaks updates.
    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = System.getenv("BARCAROLE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BARCAROLE_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("BARCAROLE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Both types take the key: the APK installed by hand deserves stable updates as much
        // as the bundle sent to the store.
        debug {
            if (uploadKeystore != null) signingConfig = signingConfigs.getByName("upload")
        }
        release {
            if (uploadKeystore != null) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.guava)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
