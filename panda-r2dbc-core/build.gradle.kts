repositories {
    mavenCentral()
}

dependencies {
    api(libs.spring.data.r2dbc)
    api(libs.r2dbc.pool)
    api(libs.bundles.jackson)
    implementation(libs.jakarta.annotation.api)
    compileOnly(libs.r2dbc.postgresql)
}

