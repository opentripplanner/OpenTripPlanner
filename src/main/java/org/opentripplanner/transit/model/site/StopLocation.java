package org.opentripplanner.transit.model.site;

import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.lang.ObjectUtils;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred to in stop times.
 */
public interface StopLocation extends LogInfo {
  /** The ID for the StopLocation */
  FeedScopedId getId();

  /** Name of the StopLocation, if provided */
  @Nullable
  I18NString getName();

  @Nullable
  I18NString getDescription();

  @Nullable
  I18NString getUrl();

  /**
   * Short text or a number that identifies the location for riders. These codes are often used in
   * phone-based reservation systems to make it easier for riders to specify a particular location.
   * The stop_code can be the same as id if it is public facing. This field should be left empty for
   * locations without a code presented to riders.
   */
  @Nullable
  default String getCode() {
    return null;
  }

  @Nullable
  default String getPlatformCode() {
    return null;
  }

  @Nullable
  default TransitMode getGtfsVehicleType() {
    return null;
  }

  @Nullable
  default String getNetexVehicleSubmode() {
    return null;
  }

  default double getLat() {
    return getCoordinate().latitude();
  }

  default double getLon() {
    return getCoordinate().longitude();
  }

  @Nullable
  default Station getParentStation() {
    return null;
  }

  @Nonnull
  default Collection<FareZone> getFareZones() {
    return List.of();
  }

  @Nonnull
  default WheelchairAccessibility getWheelchairAccessibility() {
    return WheelchairAccessibility.NO_INFORMATION;
  }

  /**
   * This is to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop.
   */
  @Nullable
  default String getFirstZoneAsString() {
    return getFareZones().stream().map(t -> t.getId().getId()).findFirst().orElse(null);
  }

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  @Nonnull
  WgsCoordinate getCoordinate();

  /**
   * The geometry of the stop.
   * <p>
   * For fixed-schedule stops this will return the same data as getCoordinate().
   * <p>
   * For flex stops this will return the geometries of the stop or group of stops.
   */
  @Nullable
  Geometry getGeometry();

  @Nullable
  default TimeZone getTimeZone() {
    return null;
  }

  boolean isPartOfStation();

  @Nonnull
  default StopTransferPriority getPriority() {
    return StopTransferPriority.ALLOWED;
  }

  boolean isPartOfSameStationAs(StopLocation alternativeStop);

  @Override
  default String logName() {
    return ObjectUtils.ifNotNull(getName(), Object::toString, null);
  }
}
