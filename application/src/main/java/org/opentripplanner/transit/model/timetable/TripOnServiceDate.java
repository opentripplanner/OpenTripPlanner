package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;

/**
 * Class for holding data about a certain trip on a certain day. Essentially a DatedServiceJourney
 * or an instance of a generic trip on a certain service date.
 */
public class TripOnServiceDate
  extends AbstractTransitEntity<TripOnServiceDate, TripOnServiceDateBuilder> {

  private final Trip trip;
  private final LocalDate serviceDate;
  private final TripAlteration tripAlteration;
  private final boolean realtimeExtraJourney;
  private final List<TripOnServiceDate> replacementFor;

  @Nullable
  private final String netexVehicleTypeId;

  TripOnServiceDate(TripOnServiceDateBuilder builder) {
    super(builder.getId());
    this.trip = builder.getTrip();
    this.serviceDate = builder.getServiceDate();
    this.tripAlteration = builder.getTripAlteration();
    this.realtimeExtraJourney = builder.isRealtimeExtraJourney();
    this.replacementFor = builder.getReplacementFor();
    this.netexVehicleTypeId = builder.getNetexVehicleTypeId();
  }

  public static TripOnServiceDateBuilder of(FeedScopedId id) {
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

  /**
   * Whether this is an extra journey, either in the planned data or added with a realtime update.
   */
  public boolean isExtraJourney() {
    return TripAlteration.EXTRA_JOURNEY.equals(tripAlteration) || realtimeExtraJourney;
  }

  boolean isRealtimeExtraJourney() {
    return realtimeExtraJourney;
  }

  public List<TripOnServiceDate> getReplacementFor() {
    return replacementFor;
  }

  /**
   * The id of the vehicle type planned to operate the trip on this service date, or the vehicle
   * type of the trip itself if the service date has none of its own.
   */
  @Nullable
  public String getNetexVehicleTypeId() {
    return netexVehicleTypeId;
  }

  @Override
  public boolean sameAs(TripOnServiceDate other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(this.trip, other.trip) &&
      Objects.equals(this.serviceDate, other.serviceDate) &&
      Objects.equals(this.tripAlteration, other.tripAlteration) &&
      this.realtimeExtraJourney == other.realtimeExtraJourney &&
      Objects.equals(this.replacementFor, other.replacementFor) &&
      Objects.equals(this.netexVehicleTypeId, other.netexVehicleTypeId)
    );
  }

  @Override
  public TripOnServiceDateBuilder copy() {
    return new TripOnServiceDateBuilder(this);
  }
}
