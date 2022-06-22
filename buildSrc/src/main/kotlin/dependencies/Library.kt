

object Version {
    const val GUAVA = "29.0-jre"
    const val FAST_JSON = "1.2.76"
    const val JACKSON = "2.12.5"
    const val DOM4J = "2.1.3"
    const val LOG4J2 = "2.15.0"
    const val LOGBACK = "1.2.7"
    const val SLF4J = "1.7.32"
    const val REACTOR = "3.4.18"

    const val PANDA_COMMONS = "1.2.6"

    const val R2DBC_POSTGRESQL = "0.8.12.RELEASE"

    const val SPRING = "2.7.0"

    const val KOTLIN_COROUTINES = "1.6.3"

}


object Library {
    const val GUAVA = "com.google.guava:guava:${Version.GUAVA}"
    const val FAST_JSON = "com.alibaba:fastjson:${Version.FAST_JSON}"
    const val LOG4J2 = "org.apache.logging.log4j:log4j-core:${Version.LOG4J2}"
    const val LOG4J2_SLF4J = "org.apache.logging.log4j:log4j-slf4j-impl:${Version.LOG4J2}"

    const val LOGBACK_CORE = "ch.qos.logback:logback-core:${Version.LOGBACK}"
    const val LOGBACK_CLASSIC = "ch.qos.logback:logback-classic:${Version.LOGBACK}"
    const val SLF4J = "org.slf4j:slf4j-api:${Version.LOGBACK}"

    /**
     * jackson的包
     * databind依赖了core和annotations
     * 所以依赖了databind就不用显式的依赖core和annotations了
     * */
    const val JACKSON_DATABIND = "com.fasterxml.jackson.core:jackson-databind:${Version.JACKSON}"
    const val JACKSON_CORE = "com.fasterxml.jackson.core:jackson-core:${Version.JACKSON}"
    const val JACKSON_ANNOTATIONS = "com.fasterxml.jackson.core:jackson-annotations:${Version.JACKSON}"

    /**
     * 序列化/反序列化java的LocalDateTime等日期类，要用jackson的这个扩展包
     * */
    const val JACKSON_DATATYPE_JSR310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Version.JACKSON}"

    /**
     * 公共库，封装了实体、服务的基类
     * 封装了常用的工具类
     * */
    const val PANDA_COMMONS = "com.jxpanda.commons:commons-base:${Version.PANDA_COMMONS}"
    const val PANDA_ENCRYPT = "com.jxpanda.commons:commons-encrypt:${Version.PANDA_COMMONS}"

    // https://mvnrepository.com/artifact/io.r2dbc/r2dbc-postgresql
    const val R2DBC_POSTGRESQL = "io.r2dbc:r2dbc-postgresql:${Version.R2DBC_POSTGRESQL}"

}