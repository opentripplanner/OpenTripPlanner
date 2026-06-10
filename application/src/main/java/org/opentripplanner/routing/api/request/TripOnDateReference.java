package org.opentripplanner.routing.api.request;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;

public sealed interface TripOnDateReference
  permits TripOnDateReferenceWithTripOnServiceDateId, TripOnDateReferenceWithTripAndDate {
  static TripOnDateReference ofTripIdAndServiceDate(FeedScopedId tripId, LocalDate serviceDate) {
    return new TripOnDateReferenceWithTripAndDate(tripId, serviceDate);
  }

  static TripOnDateReference ofTripOnServiceDateId(FeedScopedId tripOnServiceDateId) {
    return new TripOnDateReferenceWithTripOnServiceDateId(tripOnServiceDateId);
  }
}
