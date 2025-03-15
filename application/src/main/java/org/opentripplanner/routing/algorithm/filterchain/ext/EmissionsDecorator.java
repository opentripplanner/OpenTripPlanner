package org.opentripplanner.routing.algorithm.filterchain.ext;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * This qualifier is used to tag the emissions itinerary decorator. The tag is used both on the
 * provider method and the injected parameter. Note, the type is the generic {@link
 * org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator} - so the
 * type is not enough to perfome the dependecy injection. As a result the filter-chain has no
 * logical dependecy on the emmision provider at all.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface EmissionsDecorator {
}
