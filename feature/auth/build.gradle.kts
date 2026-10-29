plugins {
    alias(libs.plugins.typetype.android.compose)
}

android {
    namespace = "video.typetype.tv.feature.auth"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.zxing.core)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
}
