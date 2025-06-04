dependencies {
    implementation(project(":panda-r2dbc-core"))
    implementation(libs.r2dbc.proxy)
    implementation(libs.jackson.annotations)
    compileOnly(libs.spring.security.core)
}