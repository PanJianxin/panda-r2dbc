object SpringVersion {
    const val SPRING = "2.7.3"
    const val SPRING_DATA_RELATIONAL = "2.4.2"
    const val SPRING_R2DBC = "5.3.22"
    const val SPRING_DATA_R2DBC = "1.5.2"
}


object SpringLibrary {
    const val SPRING_BOOT = "org.springframework.boot:spring-boot-starter:${SpringVersion.SPRING}"

    const val CONFIGURATION_PROCESSOR =
        "org.springframework.boot:spring-boot-configuration-processor:${SpringVersion.SPRING}"

    // https://mvnrepository.com/artifact/org.springframework.data/spring-data-relational
    const val SPRING_DATA_RELATIONAL =
        "org.springframework.data:spring-data-relational:${SpringVersion.SPRING_DATA_RELATIONAL}"

    // https://mvnrepository.com/artifact/org.springframework/spring-r2dbc
    const val SPRING_R2DBC = "org.springframework:spring-r2dbc:${SpringVersion.SPRING_R2DBC}"

    // https://mvnrepository.com/artifact/org.springframework.data/spring-data-r2dbc
    const val SPRING_DATA_R2DBC = "org.springframework.data:spring-data-r2dbc:${SpringVersion.SPRING_DATA_R2DBC}";

}