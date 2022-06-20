package org.taruts.dynamicJava.app.dynamicWiring.proxy;

import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

/**
 * This exception is supposed to be thrown by a proxy of a dynamic implementation of a dynamic interface such as {@link DynamicComponent}.
 * A proxy throws if the dynamic delegate is not set and a method of the interface is called.
 * <p>
 * <p>
 * when a method of a dynamic interface implementation is called
 * <p>
 * if the delegate is not set. This should not happen in the process of a refresh, because
 * during a refresh the old delegate stays in place until replaced by the new one. In theory this might happen during
 * a context refresh, if the method is called
 */
public class DelegateNotSetException extends RuntimeException {
}
