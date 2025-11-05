package org.opentripplanner.ext.carpooling.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.model.site.StopType;

/**
 * Represents a stop along a carpool trip route with passenger pickup/drop-off information.
 * Each stop tracks the passenger delta (number of passengers picked up or dropped off).
 * Stops are ordered sequentially along the route.
 */
public class CarpoolStop implements StopLocation {

  /**
   * The type of carpool stop operation
   */
  public enum CarpoolStopType {
    /** Only passengers can be picked up at this stop */
    PICKUP_ONLY,
    /** Only passengers can be dropped off at this stop */
    DROP_OFF_ONLY,
    /** Both pickup and drop-off are allowed */
    PICKUP_AND_DROP_OFF,
  }

  private final AreaStop areaStop;
  private final CarpoolStopType carpoolStopType;
  private final int passengerDelta;
  private final int sequenceNumber;
  private final ZonedDateTime estimatedTime;

  /**
   * Creates a new CarpoolStop
   *
   * @param areaStop The area stop where passengers can board/alight
   * @param carpoolStopType The type of operation allowed at this stop
   * @param passengerDelta Number of passengers picked up (positive) or dropped off (negative)
   * @param sequenceNumber The order of this stop in the trip (0-based)
   * @param estimatedTime The estimated arrival/departure time at this stop
   */
  public CarpoolStop(
    AreaStop areaStop,
    CarpoolStopType carpoolStopType,
    int passengerDelta,
    int sequenceNumber,
    @Nullable ZonedDateTime estimatedTime
  ) {
    this.areaStop = areaStop;
    this.carpoolStopType = carpoolStopType;
    this.passengerDelta = passengerDelta;
    this.sequenceNumber = sequenceNumber;
    this.estimatedTime = estimatedTime;
  }

  // StopLocation interface implementation - delegate to the underlying AreaStop

  @Override
  public FeedScopedId getId() {
    return areaStop.getId();
  }

  @Override
  public int getIndex() {
    return areaStop.getIndex();
  }

  @Override
  @Nullable
  public I18NString getName() {
    return areaStop.getName();
  }

  @Override
  @Nullable
  public I18NString getDescription() {
    return areaStop.getDescription();
  }

  @Override
  @Nullable
  public I18NString getUrl() {
    return areaStop.getUrl();
  }

  @Override
  public StopType getStopType() {
    return areaStop.getStopType();
  }

  @Override
  @Nullable
  public String getCode() {
    return areaStop.getCode();
  }

  @Override
  @Nullable
  public String getPlatformCode() {
    return areaStop.getPlatformCode();
  }

  @Override
  @Nullable
  public TransitMode getVehicleType() {
    return areaStop.getVehicleType();
  }

  @Override
  public SubMode getNetexVehicleSubmode() {
    return areaStop.getNetexVehicleSubmode();
  }

  @Override
  @Nullable
  public Station getParentStation() {
    return areaStop.getParentStation();
  }

  @Override
  public Collection<FareZone> getFareZones() {
    return areaStop.getFareZones();
  }

  @Override
  public Accessibility getWheelchairAccessibility() {
    return areaStop.getWheelchairAccessibility();
  }

  @Override
  public WgsCoordinate getCoordinate() {
    return areaStop.getCoordinate();
  }

  @Override
  @Nullable
  public Geometry getGeometry() {
    return areaStop.getGeometry();
  }

  @Override
  @Nullable
  public ZoneId getTimeZone() {
    return areaStop.getTimeZone();
  }

  @Override
  public boolean isPartOfStation() {
    return areaStop.isPartOfStation();
  }

  @Override
  public StopTransferPriority getPriority() {
    return areaStop.getPriority();
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return areaStop.isPartOfSameStationAs(alternativeStop);
  }

  @Override
  @Nullable
  public List<StopLocation> getChildLocations() {
    return areaStop.getChildLocations();
  }

  @Override
  public boolean transfersNotAllowed() {
    return areaStop.transfersNotAllowed();
  }

  // Carpool-specific methods

  /**
   * @return The underlying regular stop (point-based)
   */
  public AreaStop getAreaStop() {
    return areaStop;
  }

  /**
   * @return The passenger delta at this stop. Positive values indicate pickups,
   *         negative values indicate drop-offs
   */
  public int getPassengerDelta() {
    return passengerDelta;
  }

  /**
   * @return The sequence number of this stop in the trip (0-based)
   */
  public int getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof CarpoolStop other)) return false;

    return (
      areaStop.equals(other.areaStop) &&
      carpoolStopType == other.carpoolStopType &&
      passengerDelta == other.passengerDelta &&
      sequenceNumber == other.sequenceNumber &&
      java.util.Objects.equals(estimatedTime, other.estimatedTime)
    );
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
      areaStop,
      carpoolStopType,
      passengerDelta,
      sequenceNumber,
      estimatedTime
    );
  }

  @Override
  public String toString() {
    return String.format(
      "CarpoolStop{stop=%s, type=%s, delta=%d, seq=%d, time=%s}",
      areaStop.getId(),
      carpoolStopType,
      passengerDelta,
      sequenceNumber,
      estimatedTime
    );
  }
}
