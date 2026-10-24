plugins {
    alias(libs.plugins.typetype.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "video.typetype.tv.data"
}

dependencies {
    implementation(project(":core:resources"))
    api(libs.okhttp)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    implementation(libs.google.tink.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)
}
