dependencies {
    annotationProcessor(libs.spring.configuration.processor)
    implementation(libs.spring.boot)
    implementation(project(":panda-r2dbc-extension"))
}