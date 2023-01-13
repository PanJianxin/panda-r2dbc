repositories {
    mavenCentral()
}

dependencies {
    api(SpringLibrary.SPRING_DATA_R2DBC)
    implementation(Library.JACKSON_DATABIND)
    implementation(Library.JACKSON_DATATYPE_JSR310)
    // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
    implementation("com.github.spotbugs:spotbugs-annotations:4.7.3")

}

