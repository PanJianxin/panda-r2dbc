plugins{
    `java-platform`
}

dependencies {
    constraints {
//        api(platform())
    }
}

javaPlatform {
    allowDependencies()
}