package org.opentripplanner.routing.algorithm.filterchain.ext;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * This qualifier is used to tag the {@code TaxiZoneItineraryDecorator}. The tag is used both
 * on the provider method and the injected parameter. Note, the type is the generic {@link
 * org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter} - so the
 * type is not enough to perform the dependency injection. As a result the filter-chain has no
 * logical dependency on the taxi zone provider at all.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface TaxiZoneDecorator {}
