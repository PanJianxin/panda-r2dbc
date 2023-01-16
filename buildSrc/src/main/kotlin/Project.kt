object Project {
    const val GROUP = "com.jxpanda.r2dbc"
    const val VERSION = "0.1.0"
}

object ProjectDependency {

    const val MYSQL_CONNECTOR = ":mysql-connector"

    const val PANDA_R2DBC_CORE = ":panda-r2dbc-core"
    const val PANDA_R2DBC_EXTENSION = ":panda-r2dbc-extension"
    const val PANDA_R2DBC_SPRING_DATA = ":panda-r2dbc-spring-data"

    const val PANDA_R2DBC_SPRING_BOOT = ":panda-r2dbc-spring-boot"
    const val PANDA_R2DBC_SPRING_BOOT_AUTOCONFIGURE = "$PANDA_R2DBC_SPRING_BOOT:panda-r2dbc-spring-boot-autoconfigure"
    const val PANDA_R2DBC_SPRING_BOOT_STARTER = "$PANDA_R2DBC_SPRING_BOOT:panda-r2dbc-spring-boot-starter"

    const val EXAMPLE = ":example"

}