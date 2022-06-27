package org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy;

import org.springframework.cglib.proxy.CallbackHelper;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.lang.reflect.Method;

/**
 * A factory creating a {@link DynamicComponentProxy} for a particular dynamic interface.
 * The proxy is created by means of CGLIB, repackaged in Spring Core.
 */
public class DynamicComponentProxyFactory {

    private static final DynamicInterfaceMethodInterceptor DYNAMIC_INTERFACE_METHOD_INTERCEPTOR = new DynamicInterfaceMethodInterceptor();

    /**
     * Creates a {@link DynamicComponentProxy} for a particular dynamic interface.
     * The proxy is created by means of CGLIB, repackaged in Spring Core.
     * The delegate of the created proxy is not set.
     *
     * @param <DynamicInterface> the dynamic interface implementations of which this proxy proxies
     */
    @SuppressWarnings("unused")
    public <DynamicInterface extends DynamicComponent> DynamicInterface createProxy(Class<DynamicInterface> dynamicInterface) {

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
        return (DynamicInterface) enhancer.create();
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
