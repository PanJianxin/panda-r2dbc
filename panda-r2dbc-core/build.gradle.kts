repositories {
    mavenCentral()
}

dependencies {
    api(libs.spring.data.r2dbc)
    api(libs.r2dbc.pool)
    api(libs.bundles.jackson)

    compileOnly(libs.r2dbc.postgresql)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.spring.boot)
    annotationProcessor(libs.spring.configuration.processor)
}

