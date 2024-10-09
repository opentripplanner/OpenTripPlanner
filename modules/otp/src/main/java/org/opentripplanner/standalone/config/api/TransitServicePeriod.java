package org.opentripplanner.standalone.config.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This qualifier is used to inject the TransitServicePeriod config parameter.
 */
@Qualifier
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface TransitServicePeriod {
}
