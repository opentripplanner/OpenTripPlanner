/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

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

  private final TransitMode vehicleType;

  private final SubMode netexVehicleSubmode;

  private final boolean sometimesUsedRealtime;

  private final Set<BoardingArea> boardingAreas;

  private final Set<FareZone> fareZones;

  RegularStop(RegularStopBuilder builder) {
    super(builder);
    this.index = builder.createIndex();
    this.platformCode = builder.platformCode();
    this.url = builder.url();
    this.timeZone = builder.timeZone();
    this.vehicleType = builder.vehicleType();
    this.netexVehicleSubmode = SubMode.getOrBuildAndCacheForever(builder.netexVehicleSubmode());
    this.sometimesUsedRealtime = builder.isSometimesUsedRealtime();
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
  public TransitMode getVehicleType() {
    return vehicleType;
  }

  public SubMode getNetexVehicleSubmode() {
    return netexVehicleSubmode;
  }

  /**
   * Indicates whether this stop might be used by real-time updated trips, even though it is NOT
   * used by regular scheduled trips. OTP sometimes filters out unused stops during graph build
   * or as a performance optimization. If this happens before real-time updates are applied, then
   * the routing for these stops will not work. For example this is the case with transfers
   * generation.
   * <p>
   * Common use cases:
   * <ul>
   *   <li><b>Rail platform assignment:</b> Scheduled trips may reference a limited set of platforms,
   *       while real-time updates assign trips to all available platforms. This is common when the
   *       actual platform is assigned AFTER the trips are planned.</li>
   *   <li><b>Rail Replacement Bus Services:</b> Some stops are reserved for replacement services that are
   *       added via real-time updates rather than scheduled in advance.</li>
   * </ul>
   * <p>
   * <b>FOR INTERNAL USE ONLY</b>
   * <p>
   * DO NOT EXPOSE THIS PARAMETER ON ANY API. Business logic using this feature should only use it
   * to improve routing by including these stops when stops with no trip patterns would otherwise be
   * excluded for performance reasons. Incorrectly tagging stops with this flag is not critical, it
   * will only degrade performance.
   *
   * @return {@code true} if this stop may be used by real-time trips despite having no scheduled
   *         patterns, {@code false} otherwise
   */
  public boolean isSometimesUsedRealtime() {
    return sometimesUsedRealtime;
  }

  @Override
  public Point getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(getCoordinate().asJtsCoordinate());
  }

  /**
   * Get the transfer cost priority for Stop. This will fetch the value from the parent [if parent
   * exist] or return the default value.
   */
  @SuppressWarnings("ConstantConditions")
  @Override
  public StopTransferPriority getPriority() {
    return isPartOfStation()
      ? getParentStation().getPriority()
      : StopTransferPriority.defaultValue();
  }

  @Override
  public Collection<FareZone> getFareZones() {
    return fareZones;
  }

  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas;
  }

  @Override
  public RegularStopBuilder copy() {
    return new RegularStopBuilder(this);
  }

  @Override
  public boolean sameAs(RegularStop other) {
    return (
      super.sameAs(other) &&
      Objects.equals(platformCode, other.platformCode) &&
      Objects.equals(url, other.url) &&
      Objects.equals(timeZone, other.timeZone) &&
      Objects.equals(vehicleType, other.vehicleType) &&
      Objects.equals(netexVehicleSubmode, other.netexVehicleSubmode) &&
      Objects.equals(sometimesUsedRealtime, other.sometimesUsedRealtime) &&
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
