package com.jxpanda.r2dbc.spring.data.extension.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = ExtensionProperties.PREFIX)
public class ExtensionProperties {

    public static final String PREFIX = "panda.r2dbc.extension";

    private boolean enable = false;

    private CrudEntity crudEntity = new CrudEntity();


    @Getter
    @Setter
    public static class CrudEntity {
        private boolean enable = false;
        private List<String> scanPackages;
        private boolean generateMetadata = true;
    }

}
