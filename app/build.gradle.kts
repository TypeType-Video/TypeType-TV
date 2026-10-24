plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// The release workflow reads this line: "x.y.z-beta.n" is published from the beta branch, "x.y.z" from main.
val appVersion = "0.1.1-beta.1"

// The version code follows from the name, so each release installs over the one before, and a beta sits under the
// stable release that ends it: 0.3.0-beta.2 is 30002 and 0.3.0 is 30099.
fun versionCodeOf(name: String): Int {
    val match = requireNotNull(Regex("""(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?""").matchEntire(name)) {
        "Version $name is neither x.y.z nor x.y.z-beta.n"
    }
    val (major, minor, patch, beta) = match.destructured
    require(minor.toInt() < 100 && patch.toInt() < 100) { "Minor and patch of $name stay under 100" }
    val step = if (beta.isEmpty()) 99 else beta.toInt()
    require(beta.isEmpty() || step in 1..98) { "The beta number of $name is 1 to 98" }
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 100 + step
}

android {
    namespace = "video.typetype.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "video.typetype.tv"
        minSdk = 23
        targetSdk = 37
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(project(":core:resources"))
    implementation(project(":core:data"))
    implementation(project(":core:playback"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:library"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:collections"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:player"))
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.tink.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.accessibility)
    androidTestImplementation(libs.androidx.test.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
