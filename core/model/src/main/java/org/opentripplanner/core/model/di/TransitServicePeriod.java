package org.opentripplanner.core.model.di;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This qualifier is used to inject the TransitServicePeriod config parameter.
 */
@Qualifier
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface TransitServicePeriod {}
