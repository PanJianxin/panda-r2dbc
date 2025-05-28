repositories {
    mavenCentral()
}

dependencies {
    api(libs.spring.data.r2dbc)
    implementation(libs.r2dbc.pool)
    implementation(libs.bundles.jackson)
    compileOnly(libs.db.postgresql.r2dbc)
}

