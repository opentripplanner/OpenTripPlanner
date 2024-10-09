package org.opentripplanner.framework.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is purely for documentation purposes and tells developers that a component
 * is part of sandbox code.
 */
@Target({ FIELD, METHOD, CONSTRUCTOR, TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface Sandbox {
}
