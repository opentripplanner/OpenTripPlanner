package org.opentripplanner.model;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;

/**
 * Class for holding data about a certain trip on a certain day. Essentially a DatedServiceJourney.
 */
public class TripOnServiceDate extends TransitEntity {

  private final Trip trip;
  private final LocalDate serviceDate;
  private final TripAlteration tripAlteration;
  private final List<TripOnServiceDate> replacementFor;

  public TripOnServiceDate(
    FeedScopedId id,
    Trip trip,
    LocalDate serviceDate,
    TripAlteration tripAlteration,
    List<TripOnServiceDate> replacementFor
  ) {
    super(id);
    this.trip = trip;
    this.serviceDate = serviceDate;
    this.tripAlteration = tripAlteration;
    this.replacementFor = replacementFor;
  }

  public Trip getTrip() {
    return trip;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public TripAlteration getTripAlteration() {
    return tripAlteration;
  }

  public List<TripOnServiceDate> getReplacementFor() {
    return replacementFor;
  }
}
