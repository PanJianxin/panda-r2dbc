

object KotlinVersion {
    const val KOTLIN = "1.7.0"
    const val COROUTINES = "1.6.3"
}

object KotlinLibrary {
    const val COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${KotlinVersion.COROUTINES}"
    const val COROUTINES_REACTOR = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${KotlinVersion.COROUTINES}"
}