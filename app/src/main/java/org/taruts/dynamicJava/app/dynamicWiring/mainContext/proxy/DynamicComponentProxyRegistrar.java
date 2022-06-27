package org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectsProperties;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.util.Set;

/**
 * {@link BeanDefinitionRegistryPostProcessor} registering a {@link DynamicComponentProxy} for every dynamic interface.
 */
@Component
public class DynamicComponentProxyRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Class<DynamicComponentProxyFactory> FACTORY_CLASS = DynamicComponentProxyFactory.class;
    private static final String FACTORY_BEAN_NAME = StringUtils.uncapitalize(FACTORY_CLASS.getSimpleName());

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        registerFactory(registry);

        DynamicProjectsProperties dynamicProjectsProperties = getDynamicProjectsProperties();

        dynamicProjectsProperties.forEach((projectName, projectProperties) -> {
            Set<Class<? extends DynamicComponent>> dynamicInterfaces = getDynamicInterfaces(projectProperties.getDynamicInterfacePackage());
            dynamicInterfaces.forEach(dynamicComponentInterface ->
                    registerProxy(registry, dynamicComponentInterface, projectName)
            );
        });
    }

    private void registerFactory(BeanDefinitionRegistry registry) {

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .rootBeanDefinition(FACTORY_CLASS)
                .getBeanDefinition();

        registry.registerBeanDefinition(FACTORY_BEAN_NAME, beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    /**
     * This is how we get a populated type-safe properties object being in the {@link BeanDefinitionRegistryPostProcessor} phase.
     * Autowiring does not work here.
     * <a href="https://stackoverflow.com/a/65727823/2304456">stackoverflow link</a>
     */
    private DynamicProjectsProperties getDynamicProjectsProperties() {
        BindResult<DynamicProjectsProperties> bindResult = Binder
                .get(environment)
                .bind(DynamicProjectsProperties.PREFIX, DynamicProjectsProperties.class);
        return bindResult.get();
    }

    private Set<Class<? extends DynamicComponent>> getDynamicInterfaces(String packageName) {
        Reflections reflections = new Reflections(packageName);
        return reflections.getSubTypesOf(DynamicComponent.class);
    }

    private void registerProxy(BeanDefinitionRegistry registry, Class<? extends DynamicComponent> dynamicInterface, String projectName) {
        String beanName =
                StringUtils.uncapitalize(dynamicInterface.getSimpleName()) +
                        '_' +
                        projectName.replaceAll("-", "_");

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .rootBeanDefinition(dynamicInterface)
                .setFactoryMethodOnBean("createProxy", FACTORY_BEAN_NAME)
                .addConstructorArgValue(dynamicInterface)
                .getBeanDefinition();

        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, projectName));

        registry.registerBeanDefinition(beanName, beanDefinition);
    }
}
