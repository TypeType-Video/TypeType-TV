plugins {
    alias(libs.plugins.typetype.android.compose)
}

android {
    namespace = "video.typetype.tv.core.ui"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:resources"))
    api(libs.androidx.activity.compose)
    api(libs.coil.compose)
    implementation(libs.coil.gif)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
}
