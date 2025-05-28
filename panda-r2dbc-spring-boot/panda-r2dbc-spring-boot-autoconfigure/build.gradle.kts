dependencies {
    implementation(project(":panda-r2dbc-core"))
    implementation(project(":panda-r2dbc-extension"))
    implementation(libs.r2dbc.proxy)
    annotationProcessor(libs.spring.configuration.processor)
}