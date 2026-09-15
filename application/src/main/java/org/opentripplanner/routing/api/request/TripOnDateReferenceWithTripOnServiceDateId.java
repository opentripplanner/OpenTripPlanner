package org.opentripplanner.routing.api.request;

import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A reference to a trip on date with a single id referencing the specific trip on service date.
 *
 * @param id The id of the trip on service date
 */
public record TripOnDateReferenceWithTripOnServiceDateId(FeedScopedId id) implements
  TripOnDateReference {}
