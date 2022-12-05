package org.opentripplanner.datastore.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This qualifier is used to inject the OTP base directory where the configuration files are
 * located.
 */
@Qualifier
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface OtpBaseDirectory {
}
