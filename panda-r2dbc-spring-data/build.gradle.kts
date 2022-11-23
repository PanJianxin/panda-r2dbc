repositories {
    mavenCentral()
}
plugins {
    kotlin("jvm") version KotlinVersion.KOTLIN
}


dependencies {
    api(SpringLibrary.SPRING_DATA_RELATIONAL)
//    api(SpringLibrary.SPRING_R2DBC)
    api(SpringLibrary.SPRING_DATA_R2DBC)
    implementation(Library.JACKSON_DATABIND)
    implementation(Library.JACKSON_DATATYPE_JSR310)
    implementation(Library.R2DBC_POSTGRESQL)
    implementation(KotlinLibrary.COROUTINES_CORE)
    implementation(KotlinLibrary.COROUTINES_REACTOR)
    // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
    implementation("com.github.spotbugs:spotbugs-annotations:4.7.3")

}

