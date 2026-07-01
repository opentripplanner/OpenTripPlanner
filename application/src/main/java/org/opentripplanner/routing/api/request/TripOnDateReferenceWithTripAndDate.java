package org.opentripplanner.routing.api.request;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A reference to a trip on date with an id representing the trip and a date identifying on which
 * service date it runs.
 *
 * @param id The id of the trip
 * @param serviceDate The service date of the trip
 */
public record TripOnDateReferenceWithTripAndDate(FeedScopedId id, LocalDate serviceDate) implements
  TripOnDateReference {}
