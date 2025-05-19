package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.utils.lang.ObjectUtils;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred to in stop times.
 */
public interface StopLocation extends LogInfo {
  /** The ID for the StopLocation */
  FeedScopedId getId();

  /**
   * This is the OTP internal <em>synthetic key</em>, used to reference a StopLocation inside OTP.  This is used
   * to optimize routing, we do not access the stop instance only keep the {code index}. The index will not change.
   * <p>
   * Do NOT expose this index in the APIs, it is not guaranteed to be the same across different OTP instances,
   * use the {code id} for external references.
   */
  int getIndex();

  /** Name of the StopLocation, if provided */
  @Nullable
  I18NString getName();

  @Nullable
  I18NString getDescription();

  @Nullable
  I18NString getUrl();

  StopType getStopType();

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
  default TransitMode getVehicleType() {
    return null;
  }

  default SubMode getNetexVehicleSubmode() {
    return SubMode.UNKNOWN;
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

  default Collection<FareZone> getFareZones() {
    return List.of();
  }

  default Accessibility getWheelchairAccessibility() {
    return Accessibility.NO_INFORMATION;
  }

  /**
   * This was to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop. It has now spread to several other APIs but can be removed
   * when there is a suitable replacement.
   */
  @Nullable
  default String getFirstZoneAsString() {
    for (FareZone t : getFareZones()) {
      return t.getId().getId();
    }
    return null;
  }

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
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

  /**
   * The geometry of the area that encompasses the bounds of the stop area. If the stop is defined
   * as a point, this is null.
   */
  default Optional<? extends Geometry> getEncompassingAreaGeometry() {
    return Optional.empty();
  }

  @Nullable
  default ZoneId getTimeZone() {
    return null;
  }

  boolean isPartOfStation();

  default StopTransferPriority getPriority() {
    return StopTransferPriority.defaultValue();
  }

  boolean isPartOfSameStationAs(StopLocation alternativeStop);

  /**
   * Returns the child locations of this location, for example StopLocations within a GroupStop.
   */
  @Nullable
  default List<StopLocation> getChildLocations() {
    return null;
  }

  @Override
  default String logName() {
    return ObjectUtils.ifNotNull(getName(), Object::toString, null);
  }

  /**
   * Get the parent station id if such exists. Otherwise, return the stop id.
   */
  default FeedScopedId getStationOrStopId() {
    if (this instanceof StationElement<?, ?> stationElement && stationElement.isPartOfStation()) {
      return stationElement.getParentStation().getId();
    }
    return getId();
  }

  /**
   * Whether we should allow transfers to and from stop location (other than transit)
   */
  default boolean transfersNotAllowed() {
    return false;
  }
}
