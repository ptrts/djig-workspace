package org.taruts.dynamicJava.app.dynamicWiring.proxy;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cglib.proxy.CallbackHelper;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.lang.reflect.Method;
import java.util.Set;

@Component
public class DynamicComponentsBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        Reflections reflections = new Reflections(DynamicComponent.class.getPackageName());

        Set<Class<? extends DynamicComponent>> dynamicComponentInterfaces = reflections.getSubTypesOf(DynamicComponent.class);

        dynamicComponentInterfaces.forEach(dynamicComponentInterface -> {
            DynamicComponentProxy<?> proxy = getProxy(dynamicComponentInterface);
            String beanName = StringUtils.uncapitalize(dynamicComponentInterface.getSimpleName());
            beanFactory.registerSingleton(beanName, proxy);
        });
    }

    public DynamicComponentProxy<?> getProxy(Class<? extends DynamicComponent> dynamicInterface) {

        Class<?>[] interfacesToImplement = new Class<?>[]{
                dynamicInterface
        };

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(DynamicComponentProxy.class);
        enhancer.setInterfaces(interfacesToImplement);

        DynamicInterfaceMethodInterceptor dynamicInterfaceMethodInterceptor = new DynamicInterfaceMethodInterceptor();

        CallbackHelper callbackHelper = new CallbackHelper(DynamicComponentProxy.class, interfacesToImplement) {
            @Override
            protected Object getCallback(Method method) {
                Class<?> declaringClass = method.getDeclaringClass();
                if (declaringClass == Object.class || declaringClass == DynamicComponentProxy.class) {
                    return NoOp.INSTANCE;
                } else {
                    return dynamicInterfaceMethodInterceptor;
                }
            }
        };

        enhancer.setCallbacks(callbackHelper.getCallbacks());
        enhancer.setCallbackFilter(callbackHelper);

        return (DynamicComponentProxy<?>) enhancer.create();
    }

    private static class DynamicInterfaceMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            DynamicComponentProxy<?> dynamicComponentProxy = (DynamicComponentProxy<?>) proxy;
            DynamicComponent delegate = dynamicComponentProxy.getDelegate();
            return method.invoke(delegate, args);
        }
    }
}
