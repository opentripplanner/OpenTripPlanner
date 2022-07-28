package org.opentripplanner.datastore.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This qualifier is used to inject the Google Storage Data Source Repository. Enable the
 * {@link org.opentripplanner.util.OTPFeature#GoogleCloudStorage} and the repository
 * is initialized automatically.
 */
@Qualifier
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface GoogleStorageDSRepository {
}
