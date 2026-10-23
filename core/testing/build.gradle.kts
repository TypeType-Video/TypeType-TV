plugins {
    alias(libs.plugins.typetype.android.library)
}

android {
    namespace = "video.typetype.tv.testing"
}

dependencies {
    api(project(":core:data"))
    implementation(project(":core:resources"))
}
