package com.jxpanda.r2dbc.spring.data.extension.component;

import com.jxpanda.r2dbc.spring.data.extension.config.properties.ExtensionProperties;
import com.jxpanda.r2dbc.spring.data.extension.entity.CrudEntity;
import com.jxpanda.r2dbc.spring.data.extension.service.DefaultReactiveEntityService;
import com.jxpanda.r2dbc.spring.data.extension.service.EntityServiceNameResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;

import java.util.List;

@Slf4j
public class CrudEntityRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private static final String TEMPLATE_BEAN_NAME = "reactiveEntityTemplate";

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {

        // 1. 绑定配置
        ExtensionProperties extensionProperties = Binder.get(environment)
                .bind(ExtensionProperties.PREFIX, ExtensionProperties.class)
                .orElseThrow(() -> new IllegalStateException("Failed to bind ExtensionProperties"));

        if (!extensionProperties.getCrudEntity().isEnable()) {
            // 配置没开
            return;
        }
        List<String> packages = extensionProperties.getCrudEntity().getScanPackages();
        if (packages.isEmpty()) {
            // 包列表是空的
            return;
        }

        // 2. 扫包寻找所有 AutoCrudEntity
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(CrudEntity.class));

        List<String> generatedNames = packages.stream()
                // 1. 扫每个包，把 Set<BeanDefinition> flatten 成 Stream<BeanDefinition>
                .flatMap(pkg -> scanner.findCandidateComponents(pkg).stream())
                // 2. 取出 className 并尝试加载 Class
                .map(beanDefinition -> {
                    String className = beanDefinition.getBeanClassName();
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Cannot load class " + className, e);
                    }
                })
                // 3. 只保留 CrudEntity 的子类
                .filter(CrudEntity.class::isAssignableFrom)
                // 4. 生成 (beanName, entityClass) 对
                .map(entityClass -> Pair.of(EntityServiceNameResolver.resolve(entityClass), entityClass))
                // 5. 跳过用户自定义的同名 Bean
                .filter(pair -> !registry.containsBeanDefinition(pair.getFirst()))
                // 6. 注册剩下的 BeanDefinition
                .map(pair -> {
                    String beanName = pair.getFirst();
                    Class<?> entityClass = pair.getSecond();

                    GenericBeanDefinition gbd = new GenericBeanDefinition();
                    gbd.setBeanClass(DefaultReactiveEntityService.class);
                    gbd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                    // 等同于 @Qualifier(beanName)
                    gbd.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
                    // 构造参数 0: ReactiveEntityTemplate
                    gbd.getConstructorArgumentValues()
                            .addIndexedArgumentValue(0, new RuntimeBeanReference(TEMPLATE_BEAN_NAME));
                    // 构造参数 1: entityClass
                    gbd.getConstructorArgumentValues()
                            .addIndexedArgumentValue(1, entityClass);

                    registry.registerBeanDefinition(beanName, gbd);
                    return beanName;
                }).toList();

        generateMetadata(extensionProperties, generatedNames);

    }

    private void generateMetadata(ExtensionProperties extensionProperties, List<String> generatedNames) {
        if (extensionProperties.getCrudEntity().isGenerateMetadata()) {
            String metadata = String.join(",", generatedNames);
            log.info("[CrudEntityRegistrar] Auto register ReactiveEntityService: {}", metadata);
        }
    }

}
