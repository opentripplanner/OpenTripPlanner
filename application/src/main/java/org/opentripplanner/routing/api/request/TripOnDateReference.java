package org.opentripplanner.routing.api.request;

import java.time.LocalDate;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;

/**
 * Identifies a specific trip on a specific service date. This can be specified either by a trip ID
 * and service date, or by a trip-on-service-date ID (tripOnServiceDateId) which uniquely identifies
 * a trip on a date (e.g. in NeTEx data).
 */
public record TripOnDateReference(
  @Nullable TripIdAndServiceDate tripIdOnServiceDate,
  @Nullable FeedScopedId tripOnServiceDateId
) {
  public TripOnDateReference {
    if (tripIdOnServiceDate == null && tripOnServiceDateId == null) {
      throw new IllegalArgumentException(
        "Exactly one of tripIdOnServiceDate or tripOnServiceDateId must be set"
      );
    }
    if (tripIdOnServiceDate != null && tripOnServiceDateId != null) {
      throw new IllegalArgumentException(
        "Exactly one of tripIdOnServiceDate or tripOnServiceDateId must be set"
      );
    }
  }

  public static TripOnDateReference ofTripIdAndServiceDate(
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    return new TripOnDateReference(new TripIdAndServiceDate(tripId, serviceDate), null);
  }

  public static TripOnDateReference ofTripOnServiceDateId(FeedScopedId tripOnServiceDateId) {
    return new TripOnDateReference(null, tripOnServiceDateId);
  }
}
