package org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.util.Set;

/**
 * {@link BeanDefinitionRegistryPostProcessor} registering a {@link DynamicComponentProxy} for every dynamic interface.
 */
@Component
public class DynamicComponentProxyRegistrar implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<Class<? extends DynamicComponent>> dynamicInterfaces = getDynamicInterfaces();
        dynamicInterfaces.forEach(dynamicComponentInterface ->
                registerProxy(registry, dynamicComponentInterface)
        );
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    private Set<Class<? extends DynamicComponent>> getDynamicInterfaces() {
        Reflections reflections = new Reflections(DynamicComponent.class.getPackageName());
        return reflections.getSubTypesOf(DynamicComponent.class);
    }

    private void registerProxy(BeanDefinitionRegistry registry, Class<? extends DynamicComponent> dynamicInterface) {
        String beanName = StringUtils.uncapitalize(dynamicInterface.getSimpleName());
        registry.registerBeanDefinition(
                beanName,
                BeanDefinitionBuilder
                        .genericBeanDefinition(DynamicComponentProxyFactoryBean.class)
                        .addConstructorArgValue(dynamicInterface)
                        .getBeanDefinition()
        );
    }
}
