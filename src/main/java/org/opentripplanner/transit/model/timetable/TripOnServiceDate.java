package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Class for holding data about a certain trip on a certain day. Essentially a DatedServiceJourney.
 */
public class TripOnServiceDate
  extends AbstractTransitEntity<TripOnServiceDate, TripOnServiceDateBuilder> {

  private final Trip trip;
  private final LocalDate serviceDate;
  private final TripAlteration tripAlteration;
  private final List<TripOnServiceDate> replacementFor;

  TripOnServiceDate(TripOnServiceDateBuilder builder) {
    super(builder.getId());
    this.trip = builder.getTrip();
    this.serviceDate = builder.getServiceDate();
    this.tripAlteration = builder.getTripAlteration();
    this.replacementFor = builder.getReplacementFor();
  }

  public static TripOnServiceDateBuilder of(@Nonnull FeedScopedId id) {
    return new TripOnServiceDateBuilder(id);
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

  public TripIdAndServiceDate getTripIdAndServiceDate() {
    return new TripIdAndServiceDate(trip.getId(), serviceDate);
  }

  @Override
  public boolean sameAs(@Nonnull TripOnServiceDate other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(this.trip, other.trip) &&
      Objects.equals(this.serviceDate, other.serviceDate) &&
      Objects.equals(this.tripAlteration, other.tripAlteration) &&
      Objects.equals(this.replacementFor, other.replacementFor)
    );
  }

  @Nonnull
  @Override
  public TripOnServiceDateBuilder copy() {
    return new TripOnServiceDateBuilder(this);
  }
}
