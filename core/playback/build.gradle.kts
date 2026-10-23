plugins {
    alias(libs.plugins.typetype.android.library)
}

android {
    namespace = "video.typetype.tv.playback"
}

dependencies {
    api(project(":core:data"))
    api(project(":player"))
    api(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
