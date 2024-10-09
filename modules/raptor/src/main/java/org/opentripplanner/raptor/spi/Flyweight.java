package org.opentripplanner.raptor.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This interface is used to tag methods which return flyweight objects. The implementation may
 * choose not to implement the return type as a flyweight object, but the Raptor implementation
 * is guaranteed to treat them as such - enabling the optimization.
 * <p>
 * A flyweight object is a temporary view to the state of the callee, which the caller can access
 * immediately after the object is returned, until the next method call to the callee is performed.
 * Do not store a reference to flyweight objects and make sure you do not access the state after
 * the next method call.
 * <p>
 * The flyweight design pattern is used to avoid unnecessary object creation, and the flyweight
 * object should be reused.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Flyweight {
}
