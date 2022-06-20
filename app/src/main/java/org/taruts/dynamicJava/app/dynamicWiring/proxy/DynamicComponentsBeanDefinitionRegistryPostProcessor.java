package org.taruts.dynamicJava.app.dynamicWiring.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class DynamicComponentsBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    final ClassLoader classLoader;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Reflections reflections = new Reflections(DynamicComponent.class.getPackageName());
        Set<Class<? extends DynamicComponent>> dynamicComponentInterfaces = reflections.getSubTypesOf(DynamicComponent.class);

        // Обходим все бины какие есть
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            String beanClassName = beanDefinition.getBeanClassName();

            // Получаем класс текущего бина
            Class<?> clazz;
            try {
                clazz = Class.forName(beanClassName, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            // Смотрим, имеет ли он отношение к динамическим интерфейсам
            if (DynamicComponent.class.isAssignableFrom(clazz)) {

                // Получаем какой-либо из его динамических интерфейсов
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

                    // Этот класс имплементирует один из динамических интерфейсов
                    // Это может быть прокся из основного контекста, или делегат из дочернего

                    boolean isProxy = DynamicComponentProxy.class.isAssignableFrom(clazz);
                    if (isProxy) {
                        throw new IllegalStateException("" +
                                "DynamicComponentProxy beans can only be in the main context. " +
                                "This BeanDefinitionRegistryPostProcessor and the BeanDefinitionRegistry (the argument in this callback) " +
                                "are of the child context " +
                                "so the BeanDefinitionRegistry should not see beans from the main context"
                        );
                    } else {
                        // Если динамический компонент хочет заинжектить к себе другой,
                        // то он должен получить не его проксю из основного контекста, а исходный компонент
                        beanDefinition.setPrimary(true);
                    }
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
