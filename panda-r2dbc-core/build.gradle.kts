repositories {
    mavenCentral()
}

dependencies {
    api(libs.spring.data.r2dbc)
    api(libs.r2dbc.pool)
    implementation(libs.bundles.jackson)
    implementation(libs.jakarta.annotation.api)
}

