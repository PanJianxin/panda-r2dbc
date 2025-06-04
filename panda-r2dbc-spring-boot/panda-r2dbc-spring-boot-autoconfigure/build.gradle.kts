dependencies {
    implementation(project(":panda-r2dbc-core"))
    implementation(project(":panda-r2dbc-extension"))
    implementation(libs.r2dbc.proxy)
    compileOnly(libs.spring.security.core)
    compileOnly(libs.spring.security.oauth2.jose)
    annotationProcessor(libs.spring.configuration.processor)
}