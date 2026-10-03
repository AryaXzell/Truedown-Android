import com.android.build.OutputFile
import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.androidx.baselineprofile)
}

val releaseKeystorePath = System.getenv("KEYSTORE_FILE") ?: ""
val releaseKeystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
val releaseKeyAlias = System.getenv("KEY_ALIAS") ?: "truedown"
val releaseKeyPassword = System.getenv("KEY_PASSWORD") ?: releaseKeystorePassword

val hasReleaseSigning = releaseKeystorePath.isNotBlank() &&
        releaseKeystorePassword.isNotBlank() &&
        file(releaseKeystorePath).exists()

val appVersionMajor = 1
val appVersionMinor = 1
val appVersionPatch = 1
val baseVersionCode = appVersionMajor * 10000 + appVersionMinor * 100 + appVersionPatch
val appVersionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"

// Auto-increment build number per build to avoid Android version conflict / downgrade errors
val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val localTimestampOffset = ((System.currentTimeMillis() - 1700000000000L) / 60000L).toInt().coerceAtLeast(1)
val dynamicBuildNumber = ciRunNumber ?: (localTimestampOffset % 100000)
val dynamicBaseVersionCode = (baseVersionCode * 100000) + dynamicBuildNumber

val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

android {
    namespace = "com.aryaxzell.truedown"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aryaxzell.truedown"
        minSdk = 29
        targetSdk = 35
        versionCode = (dynamicBaseVersionCode * 10) // default universal (code 0)
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    val rootDebugKeystore = file("${project.rootDir}/debug.keystore")

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        if (rootDebugKeystore.exists()) {
            getByName("debug") {
                storeFile = rootDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else if (rootDebugKeystore.exists()) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            if (rootDebugKeystore.exists()) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    applicationVariants.all {
        val variant = this
        if (variant.buildType.name == "release") {
            variant.outputs.all {
                val output = this as ApkVariantOutputImpl
                val abiFilter = output.getFilter(OutputFile.ABI)
                val abiCode = abiCodes[abiFilter] ?: 0
                output.versionCodeOverride = (dynamicBaseVersionCode * 10) + abiCode

                val abiName = abiFilter ?: "universal"
                output.outputFileName = "truedown-${variant.versionName}-$abiName.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    baselineProfile {
        filter {
            include("com.aryaxzell.truedown.**")
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.androidx.profileinstaller)

    ksp(libs.androidx.room.compiler)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
