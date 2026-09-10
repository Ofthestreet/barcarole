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

/**
 * The Play Store refuses an upload whose version code it has already seen, and never accepts a
 * lower one, so the code is derived from the version name rather than typed by hand: 1.2.3
 * becomes 10203. Minor and patch therefore have to stay below 100.
 */
fun versionCodeOf(name: String): Int {
    val parts = name.removePrefix("v").substringBefore('-').split('.')
    fun part(index: Int) = parts.getOrNull(index)?.toIntOrNull() ?: 0
    return part(0) * 10_000 + part(1) * 100 + part(2)
}

// A tagged release passes its version in. Every other build carries the version under
// development plus a suffix naming the build it came from, so the About screen distinguishes
// a published version from a trial build - without moving the version code, which stays derived from
// the numbers alone and therefore identical across a release and its development builds.
val developmentVersion = "0.2.0"
val appVersionName: String = System.getenv("BARCAROLE_VERSION_NAME")
    ?: listOfNotNull(developmentVersion, System.getenv("BARCAROLE_VERSION_SUFFIX")).joinToString("-")
val appVersionCode: Int = versionCodeOf(appVersionName)

android {
    namespace = "io.github.ofthestreet.barcarole"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ofthestreet.barcarole"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
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
