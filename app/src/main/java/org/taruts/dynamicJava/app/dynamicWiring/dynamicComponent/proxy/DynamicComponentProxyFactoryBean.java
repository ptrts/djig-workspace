package org.taruts.dynamicJava.app.dynamicWiring.dynamicComponent.proxy;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.CallbackHelper;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.lang.reflect.Method;

/**
 * {@link FactoryBean} creating a {@link DynamicComponentProxy} for a particular dynamic interface.
 * The proxy is created by means of CGLIB, repackaged in Spring Core.
 *
 * @param <DynamicInterface> the dynamic interface implementations of which this proxy proxies
 */
public class DynamicComponentProxyFactoryBean<DynamicInterface extends DynamicComponent> implements FactoryBean<DynamicComponentProxy<DynamicInterface>> {

    private static final DynamicInterfaceMethodInterceptor DYNAMIC_INTERFACE_METHOD_INTERCEPTOR = new DynamicInterfaceMethodInterceptor();

    private final Class<DynamicInterface> dynamicInterface;

    /**
     * @param dynamicInterface The dynamic interface the created bean will proxy
     */
    public DynamicComponentProxyFactoryBean(Class<DynamicInterface> dynamicInterface) {
        this.dynamicInterface = dynamicInterface;
    }

    @Override
    public Class<DynamicInterface> getObjectType() {
        return dynamicInterface;
    }

    @Override
    public DynamicComponentProxy<DynamicInterface> getObject() {

        // Here's a good article about how to use CGLIB
        //https://www.javacodegeeks.com/2013/12/cglib-the-missing-manual.html

        Class<?>[] interfacesToImplement = new Class<?>[]{
                dynamicInterface
        };

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(DynamicComponentProxy.class);
        enhancer.setInterfaces(interfacesToImplement);

        CallbackHelper callbackHelper = new CallbackHelper(DynamicComponentProxy.class, interfacesToImplement) {
            @Override
            protected Object getCallback(Method method) {
                Class<?> declaringClass = method.getDeclaringClass();
                if (declaringClass == dynamicInterface) {
                    return DYNAMIC_INTERFACE_METHOD_INTERCEPTOR;
                } else {
                    return NoOp.INSTANCE;
                }
            }
        };

        enhancer.setCallbacks(callbackHelper.getCallbacks());
        enhancer.setCallbackFilter(callbackHelper);

        //noinspection unchecked
        return (DynamicComponentProxy<DynamicInterface>) enhancer.create();
    }

    /**
     * Interceptor used to proxy dynamic interface methods.
     * Passes all dynamic interface calls to the delegate defined in the proxy.
     */
    private static class DynamicInterfaceMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            DynamicComponentProxy<?> dynamicComponentProxy = (DynamicComponentProxy<?>) proxy;
            DynamicComponent delegate = dynamicComponentProxy.getDelegate();
            return method.invoke(delegate, args);
        }
    }
}
