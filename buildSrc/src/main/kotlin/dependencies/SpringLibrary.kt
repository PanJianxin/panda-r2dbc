
object SpringVersion {
    const val SPRING_DATA_RELATIONAL = "2.4.0"
    const val SPRING_R2DBC = "5.3.20"
}

object SpringLibrary {
    const val SPRING_BOOT = "org.springframework.boot:spring-boot-starter:${Version.SPRING}"

    const val CONFIGURATION_PROCESSOR = "org.springframework.boot:spring-boot-configuration-processor:${Version.SPRING}"

    // https://mvnrepository.com/artifact/org.springframework.data/spring-data-relational
    const val SPRING_DATA_RELATIONAL =
        "org.springframework.data:spring-data-relational:${SpringVersion.SPRING_DATA_RELATIONAL}"

    // https://mvnrepository.com/artifact/org.springframework/spring-r2dbc
    const val SPRING_R2DBC = "org.springframework:spring-r2dbc:${SpringVersion.SPRING_R2DBC}"
}