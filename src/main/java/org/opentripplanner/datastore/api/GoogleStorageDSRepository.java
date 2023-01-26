package org.opentripplanner.datastore.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import org.opentripplanner.framework.application.OTPFeature;

/**
 * This qualifier is used to inject the Google Storage Data Source Repository. Enable the
 * {@link OTPFeature#GoogleCloudStorage} and the repository
 * is initialized automatically.
 */
@Qualifier
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface GoogleStorageDSRepository {
}
