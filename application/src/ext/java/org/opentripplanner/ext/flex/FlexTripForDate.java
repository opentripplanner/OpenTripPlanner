package org.opentripplanner.ext.flex;

import java.time.LocalDate;
import java.util.Objects;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public final class FlexTripForDate {

  private final LocalDate serviceDate;
  private final LocalDate endOfRunningPeriod;
  private final FlexTrip<?, ?> flexTrip;

  public FlexTripForDate(
    LocalDate serviceDate,
    LocalDate endOfRunningPeriod,
    FlexTrip<?, ?> flexTrip
  ) {
    this.serviceDate = serviceDate;
    this.endOfRunningPeriod = endOfRunningPeriod;
    this.flexTrip = flexTrip;
  }

  /** The service date of the trip pattern. */
  public LocalDate serviceDate() {
    return serviceDate;
  }

  /** The running date on which the last trip arrives.  */
  public LocalDate endOfRunningPeriod() {
    return endOfRunningPeriod;
  }

  /** The FlexTrip that runs on the service date. */
  public FlexTrip<?, ?> flexTrip() {
    return flexTrip;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FlexTripForDate) obj;
    return (
      Objects.equals(this.serviceDate, that.serviceDate) &&
      Objects.equals(this.endOfRunningPeriod, that.endOfRunningPeriod) &&
      Objects.equals(this.flexTrip, that.flexTrip)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceDate, endOfRunningPeriod, flexTrip);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FlexTripForDate.class)
      .addDate("serviceDate", serviceDate)
      .addDate("endOfRunningPeriod", endOfRunningPeriod)
      .addObj("flexTrip", flexTrip)
      .toString();
  }
}
