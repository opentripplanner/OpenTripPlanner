package org.opentripplanner.model;

import java.util.Objects;
import org.opentripplanner.model.calendar.ServiceDate;

/**
 * Class to use as key in HashMap containing feed id, trip id and service date
 */
public record TripIdAndServiceDate(FeedScopedId tripId, ServiceDate serviceDate) {
  @Override
  public int hashCode() {
    return Objects.hash(tripId, serviceDate);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TripIdAndServiceDate other = (TripIdAndServiceDate) obj;
    return (
      Objects.equals(this.tripId, other.tripId) &&
      Objects.equals(this.serviceDate, other.serviceDate)
    );
  }
}
