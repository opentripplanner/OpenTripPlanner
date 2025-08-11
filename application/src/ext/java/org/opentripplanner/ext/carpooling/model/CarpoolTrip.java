package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

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
  private final Trip trip;
  private final String provider;
  private final int availableSeats;

  public CarpoolTrip(CarpoolTripBuilder builder) {
    super(builder.getId());
    this.boardingArea = builder.getBoardingArea();
    this.alightingArea = builder.getAlightingArea();
    this.startTime = builder.getStartTime();
    this.endTime = builder.getEndTime();
    this.trip = builder.getTrip();
    this.provider = builder.getProvider();
    this.availableSeats = builder.getAvailableSeats();
  }

  public AreaStop getBoardingArea() {
    return boardingArea;
  }

  public AreaStop getAlightingArea() {
    return alightingArea;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public ZonedDateTime getEndTime() {
    return endTime;
  }

  public Trip getTrip() {
    return trip;
  }

  public String getProvider() {
    return provider;
  }

  public int getAvailableSeats() {
    return availableSeats;
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
