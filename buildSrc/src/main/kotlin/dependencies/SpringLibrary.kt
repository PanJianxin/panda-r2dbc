object SpringVersion {
    const val SPRING = "3.0.6"
    const val SPRING_DATA_R2DBC = "3.0.5"
}


object SpringLibrary {
    const val SPRING_BOOT = "org.springframework.boot:spring-boot-starter:${SpringVersion.SPRING}"

    const val CONFIGURATION_PROCESSOR =
        "org.springframework.boot:spring-boot-configuration-processor:${SpringVersion.SPRING}"

    // https://mvnrepository.com/artifact/org.springframework.data/spring-data-r2dbc
    const val SPRING_DATA_R2DBC = "org.springframework.data:spring-data-r2dbc:${SpringVersion.SPRING_DATA_R2DBC}";



}