package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopType;

/**
 * Represents a stop along a carpool trip route with passenger pickup/drop-off information.
 * Each stop tracks the passenger delta (number of passengers picked up or dropped off).
 * Stops are ordered sequentially along the route.
 */
public class CarpoolStop
  extends AbstractTransitEntity<CarpoolStop, CarpoolStopBuilder>
  implements StopLocation {

  private final int index;
  private final I18NString name;
  private final I18NString description;
  private final I18NString url;
  private final WgsCoordinate centroid;
  private final Geometry geometry;
  private final CarpoolStopType carpoolStopType;
  private final ZonedDateTime expectedArrivalTime;
  private final ZonedDateTime aimedArrivalTime;
  private final ZonedDateTime expectedDepartureTime;
  private final ZonedDateTime aimedDepartureTime;
  private final int sequenceNumber;
  private final int passengerDelta;

  public CarpoolStop(CarpoolStopBuilder builder) {
    super(builder.getId());
    this.index = builder.createIndex();
    // according to the spec stop location names are optional for flex zones so, we set the id
    // as the bogus name. *shrug*
    if (builder.name() == null) {
      this.name = new NonLocalizedString(builder.getId().toString());
    } else {
      this.name = builder.name();
    }
    this.description = builder.description();
    this.url = builder.url();
    this.centroid = Objects.requireNonNull(builder.centroid());
    this.geometry = builder.geometry();
    this.carpoolStopType = builder.carpoolStopType();
    this.expectedArrivalTime = builder.expectedArrivalTime();
    this.aimedArrivalTime = builder.aimedArrivalTime();
    this.expectedDepartureTime = builder.expectedDepartureTime();
    this.aimedDepartureTime = builder.aimedDepartureTime();
    this.sequenceNumber = builder.sequenceNumber();
    this.passengerDelta = builder.passengerDelta();
  }

  public static CarpoolStopBuilder of(FeedScopedId id, IntSupplier indexCounter) {
    return new CarpoolStopBuilder(id, indexCounter);
  }

  public static CarpoolStopBuilder of(CarpoolStop carpoolStop, IntSupplier indexCounter) {
    return new CarpoolStopBuilder(carpoolStop);
  }

  // StopLocation interface implementation - delegate to the underlying AreaStop

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  @Nullable
  public I18NString getName() {
    return name;
  }

  @Override
  @Nullable
  public I18NString getDescription() {
    return description;
  }

  @Override
  @Nullable
  public I18NString getUrl() {
    return url;
  }

  @Override
  public StopType getStopType() {
    return StopType.REGULAR;
  }

  @Override
  @Nullable
  public String getCode() {
    return null;
  }

  @Override
  @Nullable
  public String getPlatformCode() {
    return null;
  }

  @Override
  public WgsCoordinate getCoordinate() {
    return centroid;
  }

  @Override
  @Nullable
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  // Carpool-specific methods

  /**
   * @return The type of carpool operation allowed at this stop
   */
  public CarpoolStopType getCarpoolStopType() {
    return carpoolStopType;
  }

  /**
   * @return The expected arrival time, or null if not applicable (e.g., origin stop)
   */
  @Nullable
  public ZonedDateTime getExpectedArrivalTime() {
    return expectedArrivalTime;
  }

  /**
   * @return The aimed arrival time, or null if not applicable (e.g., origin stop)
   */
  @Nullable
  public ZonedDateTime getAimedArrivalTime() {
    return aimedArrivalTime;
  }

  /**
   * @return The expected departure time, or null if not applicable (e.g., destination stop)
   */
  @Nullable
  public ZonedDateTime getExpectedDepartureTime() {
    return expectedDepartureTime;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  /**
   * @return The aimed departure time, or null if not applicable (e.g., destination stop)
   */
  @Nullable
  public ZonedDateTime getAimedDepartureTime() {
    return aimedDepartureTime;
  }

  /**
   * Returns the primary timing for this stop, preferring aimed arrival time.
   * This provides backward compatibility for code that expects a single time value.
   *
   * @return The aimed arrival time if set, otherwise aimed departure time
   */
  @Nullable
  public ZonedDateTime getEstimatedTime() {
    return aimedArrivalTime != null ? aimedArrivalTime : aimedDepartureTime;
  }

  public int getPassengerDelta() {
    return passengerDelta;
  }

  @Override
  public boolean sameAs(CarpoolStop other) {
    return false;
  }

  @Override
  public CarpoolStopBuilder copy() {
    return new CarpoolStopBuilder(this);
  }
}
