dependencies {
    annotationProcessor(SpringLibrary.CONFIGURATION_PROCESSOR)
    implementation(SpringLibrary.SPRING_BOOT)
    implementation(project(ProjectDependency.PANDA_R2DBC_EXTENSION))
}