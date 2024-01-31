/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class RegularStop
  extends StationElement<RegularStop, RegularStopBuilder>
  implements StopLocation {

  private final int index;
  private final String platformCode;

  private final I18NString url;

  private final ZoneId timeZone;

  private final TransitMode gtfsVehicleType;

  private final SubMode netexVehicleSubmode;

  private final Set<BoardingArea> boardingAreas;

  private final Set<FareZone> fareZones;

  RegularStop(RegularStopBuilder builder) {
    super(builder);
    this.index = builder.createIndex();
    this.platformCode = builder.platformCode();
    this.url = builder.url();
    this.timeZone = builder.timeZone();
    this.gtfsVehicleType = builder.vehicleType();
    this.netexVehicleSubmode = SubMode.getOrBuildAndCacheForever(builder.netexVehicleSubmode());
    this.boardingAreas = setOfNullSafe(builder.boardingAreas());
    this.fareZones = setOfNullSafe(builder.fareZones());
    if (isPartOfStation()) {
      getParentStation().addChildStop(this);
    }
  }

  public static RegularStopBuilder of(FeedScopedId id, IntSupplier indexCounter) {
    return new RegularStopBuilder(id, indexCounter);
  }

  @Override
  public int getIndex() {
    return index;
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

  @Nonnull
  @Override
  public StopType getStopType() {
    return StopType.REGULAR;
  }

  @Override
  @Nullable
  public ZoneId getTimeZone() {
    if (timeZone != null) {
      return timeZone;
    } else if (isPartOfStation()) {
      return getParentStation().getTimezone();
    }
    return null;
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

  @Nonnull
  public SubMode getNetexVehicleSubmode() {
    return netexVehicleSubmode;
  }

  @Override
  @Nonnull
  public Point getGeometry() {
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
    return fareZones;
  }

  @Nonnull
  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas;
  }

  @Override
  @Nonnull
  public RegularStopBuilder copy() {
    return new RegularStopBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull RegularStop other) {
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

  @Override
  public boolean transfersNotAllowed() {
    var parentStation = getParentStation();
    return parentStation != null && parentStation.isTransfersNotAllowed();
  }
}
