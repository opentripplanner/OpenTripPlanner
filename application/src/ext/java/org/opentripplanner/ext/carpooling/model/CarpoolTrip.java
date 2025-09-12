package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A carpool trip is defined by two area stops and a start time, in addition to all the other fields
 * that are necessary for a valid Trip. It is created from SIRI ET messages that contain the
 * necessary identifiers and trip information.
 */
public class CarpoolTrip
  extends AbstractTransitEntity<CarpoolTrip, CarpoolTripBuilder>
  implements LogInfo {

  private final AreaStop boardingArea;
  private final AreaStop alightingArea;
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final String provider;

  // The amount of time the trip can deviate from the scheduled time in order to pick up or drop off
  // a passenger.
  private final Duration deviationBudget;
  private final int availableSeats;

  public CarpoolTrip(CarpoolTripBuilder builder) {
    super(builder.getId());
    this.boardingArea = builder.boardingArea();
    this.alightingArea = builder.alightingArea();
    this.startTime = builder.startTime();
    this.endTime = builder.endTime();
    this.provider = builder.provider();
    this.availableSeats = builder.availableSeats();
    this.deviationBudget = builder.deviationBudget();
  }

  public AreaStop boardingArea() {
    return boardingArea;
  }

  public AreaStop alightingArea() {
    return alightingArea;
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public String provider() {
    return provider;
  }

  public Duration deviationBudget() {
    return deviationBudget;
  }

  public int availableSeats() {
    return availableSeats;
  }

  public Duration tripDuration() {
    // Since the endTime is set by the driver at creation, we subtract the deviationBudget to get the
    // actual trip duration.
    return Duration.between(startTime, endTime).minus(deviationBudget);
  }

  @Nullable
  @Override
  public String logName() {
    return getId().toString();
  }

  @Override
  public boolean sameAs(CarpoolTrip other) {
    return (
      getId().equals(other.getId()) &&
      boardingArea.equals(other.boardingArea) &&
      alightingArea.equals(other.alightingArea) &&
      startTime.equals(other.startTime) &&
      endTime.equals(other.endTime)
    );
  }

  @Override
  public TransitBuilder<CarpoolTrip, CarpoolTripBuilder> copy() {
    return new CarpoolTripBuilder(this);
  }
}
