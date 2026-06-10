package org.opentripplanner.netex.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation for code specific to the Swiss NeTEx profile (or data).
 */
@Target({ ElementType.TYPE_USE, ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface SwissProfile {}
