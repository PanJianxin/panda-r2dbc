rootProject.name = "panda-r2dbc"
include("mysql-connector")
include("example")
include("panda-r2dbc-core")
include("panda-r2dbc-extension")
include("panda-r2dbc-spring-boot")

//include("panda-r2dbc-spring-data")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")
findProject(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")?.name = "panda-r2dbc-spring-boot-autoconfigure"
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")
findProject(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")?.name = "panda-r2dbc-spring-boot-starter"
