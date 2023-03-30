package org.opentripplanner.framework.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ FIELD, METHOD, CONSTRUCTOR })
@Retention(RetentionPolicy.SOURCE)
public @interface Sandbox {
}
