object Bom {
    const val R2DBC = "io.r2dbc:r2dbc-bom:Borca-SR2"
    const val REACTOR = "io.projectreactor:reactor-bom:2022.0.0"
    const val NETTY = "io.netty:netty-bom:4.1.85.Final"
}

/**
 * 使用BOM管理的依赖， 不需要写版本号
 * */
object BomLibrary {
    /**
     * https://mvnrepository.com/artifact/io.projectreactor/reactor-core
     * */
    const val REACTOR = "io.projectreactor:reactor-core"
    const val REACTOR_NETTY = "io.projectreactor.netty:reactor-netty"

    const val R2DBC_SPI = "io.r2dbc:r2dbc-spi"
    const val R2DBC_POOL = "io.r2dbc:r2dbc-pool"
    const val R2DBC_PROXY = "io.r2dbc:r2dbc-proxy"

    const val NETTY_HANDLER = "io.netty:netty-handler"
}