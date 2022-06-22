package org.taruts.dynamicJava.app.dynamicWiring.childContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy.DynamicComponentProxy;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.util.Arrays;
import java.util.Set;

/**
 * A {@link BeanDefinitionRegistryPostProcessor} enabling dynamic components to inject their fellow dynamic components
 * themselves rather than their proxies from the main context.
 * This is done by making every dynamic component bean in the child context @{@link Primary}.
 * The class must be used with the dynamic child Spring context only, not with the main context.
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicComponentDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    final ClassLoader classLoader;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Reflections reflections = new Reflections(DynamicComponent.class.getPackageName());
        Set<Class<? extends DynamicComponent>> dynamicComponentInterfaces = reflections.getSubTypesOf(DynamicComponent.class);

        // Looping through all the beans in the registry
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            Class<?> clazz = getBeanClass(beanDefinition);

            // Does it implement the marker interface DynamicComponent (which every dynamic component interface must extend)?
            if (DynamicComponent.class.isAssignableFrom(clazz)) {

                // Now we ensure that the bean class implements a dynamic component interface that extends DynamicComponent.
                Class<? extends DynamicComponent> firstDynamicInterface = Arrays
                        .stream(clazz.getInterfaces())
                        .filter(DynamicComponent.class::isAssignableFrom)
                        .map(currentInterface -> {
                            //noinspection unchecked
                            return (Class<? extends DynamicComponent>) currentInterface;
                        })
                        .filter(dynamicComponentInterfaces::contains)
                        .findFirst()
                        .orElse(null);

                if (firstDynamicInterface != null) {
                    boolean isProxy = DynamicComponentProxy.class.isAssignableFrom(clazz);
                    if (isProxy) {
                        throw new IllegalStateException("" +
                                "DynamicComponentProxy beans can only be in the main context. " +
                                "This BeanDefinitionRegistryPostProcessor and the BeanDefinitionRegistry (the argument in this callback) " +
                                "are of the child context " +
                                "so the BeanDefinitionRegistry should not see beans from the main context"
                        );
                    } else {
                        beanDefinition.setPrimary(true);
                    }
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    private Class<?> getBeanClass(BeanDefinition beanDefinition) {
        Class<?> clazz;
        try {
            String beanClassName = beanDefinition.getBeanClassName();
            clazz = Class.forName(beanClassName, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return clazz;
    }
}
