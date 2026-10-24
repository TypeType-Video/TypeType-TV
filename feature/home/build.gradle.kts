plugins {
    alias(libs.plugins.typetype.android.compose)
}

android {
    namespace = "video.typetype.tv.feature.home"
}

dependencies {
    implementation(project(":core:ui"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
}
