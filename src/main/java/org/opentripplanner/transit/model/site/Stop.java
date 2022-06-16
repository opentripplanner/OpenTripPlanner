/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.I18NString;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends StationElement<Stop, StopBuilder> implements StopLocation {

  private final String platformCode;

  private final I18NString url;

  private final TimeZone timeZone;

  private final TransitMode gtfsVehicleType;

  private final String netexVehicleSubmode;

  private final Set<BoardingArea> boardingAreas;

  private final Set<FareZone> fareZones;

  Stop(StopBuilder builder) {
    super(builder);
    this.platformCode = builder.platformCode();
    this.url = builder.url();
    this.timeZone = builder.timeZone();
    this.gtfsVehicleType = builder.vehicleType();
    this.netexVehicleSubmode = builder.netexSubmode();
    this.boardingAreas = setOfNullSafe(builder.boardingAreas());
    this.fareZones = setOfNullSafe(builder.fareZones());
    if (isPartOfStation()) {
      getParentStation().addChildStop(this);
    }
  }

  public static StopBuilder of(FeedScopedId id) {
    return new StopBuilder(id);
  }

  /**
   * Platform identifier for a platform/stop belonging to a station. This should be just the
   * platform identifier (eg. "G" or "3").
   */
  @Override
  @Nullable
  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * URL to a web page containing information about this particular stop.
   */
  @Override
  @Nullable
  public I18NString getUrl() {
    return url;
  }

  @Override
  @Nullable
  public TimeZone getTimeZone() {
    return timeZone;
  }

  /**
   * Used for describing the type of transportation used at the stop. This can be used eg. for
   * deciding how to render a stop when it is used by multiple routes with different vehicle types.
   */
  @Override
  @Nullable
  public TransitMode getGtfsVehicleType() {
    return gtfsVehicleType;
  }

  @Override
  @Nullable
  public String getNetexVehicleSubmode() {
    return netexVehicleSubmode;
  }

  @Override
  @Nullable
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(getCoordinate().asJtsCoordinate());
  }

  /**
   * Get the transfer cost priority for Stop. This will fetch the value from the parent [if parent
   * exist] or return the default value.
   */
  @SuppressWarnings("ConstantConditions")
  @Override
  @Nonnull
  public StopTransferPriority getPriority() {
    return isPartOfStation() ? getParentStation().getPriority() : StopTransferPriority.ALLOWED;
  }

  @Override
  @Nonnull
  public Collection<FareZone> getFareZones() {
    return Collections.unmodifiableCollection(fareZones);
  }

  @Nonnull
  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas;
  }

  @Override
  @Nonnull
  public StopBuilder copy() {
    return new StopBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull Stop other) {
    return (
      super.sameAs(other) &&
      Objects.equals(platformCode, other.platformCode) &&
      Objects.equals(url, other.url) &&
      Objects.equals(timeZone, other.timeZone) &&
      Objects.equals(gtfsVehicleType, other.gtfsVehicleType) &&
      Objects.equals(netexVehicleSubmode, other.netexVehicleSubmode) &&
      Objects.equals(boardingAreas, other.boardingAreas) &&
      Objects.equals(fareZones, other.fareZones)
    );
  }
}
