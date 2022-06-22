rootProject.name = "panda-r2dbc"
include("mysql-connector")
include("panda-r2dbc-spring-boot")
include("example")
include("panda-r2dbc-spring-data")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")
findProject(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")?.name = "panda-r2dbc-spring-boot-autoconfigure"
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")
findProject(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")?.name = "panda-r2dbc-spring-boot-starter"
