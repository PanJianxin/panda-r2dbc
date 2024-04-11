# panda-r2dbc

### 开发背景
因为在开发过程中用了一段时间的spring-webflux之后，就很上头

### 介绍
R2DBC的库

一个基于[Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc#overview)的ORM，依赖了spring-data-r2dbc然后做了一些增强功能，只做了增强，没做改变，因此原spring-data-r2dbc的功能都有[文档链接](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/)。


整个项目主要分为以下几个包

panda-r2dbc-core，这个包实现了核心的增强功能，具体功能会补一个详细文档来描述
panda-r2dbc-extension，这个包是我日常工作中开发常用的一些扩展功能，这部分的开发还没有完成
panda-r2dbc-spring-boot，这是一个spring-boot-starter，快速集成到spring项目中使用
example，这是开发过程中测试用的子项目


