/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import static org.opentripplanner.model.WheelChairBoarding.NO_INFORMATION;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends StationElement implements StopLocation {

  private static final long serialVersionUID = 2L;

  private final Collection<FareZone> fareZones;

  /**
   * Platform identifier for a platform/stop belonging to a station. This should be just the
   * platform identifier (eg. "G" or "3").
   */
  private final String platformCode;

  /**
   * URL to a web page containing information about this particular stop.
   */
  private final I18NString url;

  private final TimeZone timeZone;

  /**
   * Used for describing the type of transportation used at the stop. This can be used eg. for
   * deciding how to render a stop when it is used by multiple routes with different vehicle types.
   */
  private final TransitMode vehicleType;

  private final String netexSubmode;

  private HashSet<BoardingArea> boardingAreas;

  public Stop(
    FeedScopedId id,
    I18NString name,
    String code,
    String description,
    WgsCoordinate coordinate,
    WheelChairBoarding wheelchairBoarding,
    StopLevel level,
    String platformCode,
    Collection<FareZone> fareZones,
    I18NString url,
    TimeZone timeZone,
    TransitMode vehicleType,
    String netexSubmode
  ) {
    super(id, name, code, description, coordinate, wheelchairBoarding, level);
    this.platformCode = platformCode;
    this.fareZones = fareZones;
    this.url = url;
    this.timeZone = timeZone;
    this.vehicleType = vehicleType;
    this.netexSubmode = netexSubmode;
  }

  public static Stop stopForTest(
    String idAndName,
    WheelChairBoarding wheelChairBoarding,
    double lat,
    double lon
  ) {
    return stopForTest(idAndName, null, lat, lon, null, wheelChairBoarding);
  }

  /**
   * @see #stopForTest(String, double, double, Station)
   */
  public static Stop stopForTest(String idAndName, double lat, double lon) {
    return stopForTest(idAndName, null, lat, lon, null, NO_INFORMATION);
  }

  /**
   * @see #stopForTest(String, double, double, Station)
   */
  public static Stop stopForTest(String idAndName, String desc, double lat, double lon) {
    return stopForTest(idAndName, desc, lat, lon, null, NO_INFORMATION);
  }

  /**
   * Create a minimal Stop object for unit-test use, where the test only care about id, name and
   * coordinate. The feedId is static set to "F"
   */
  public static Stop stopForTest(String idAndName, double lat, double lon, Station parent) {
    return stopForTest(idAndName, null, lat, lon, parent, NO_INFORMATION);
  }

  public static Stop stopForTest(
    String idAndName,
    String desc,
    double lat,
    double lon,
    Station parent
  ) {
    return stopForTest(idAndName, desc, lat, lon, parent, null);
  }

  /**
   * Create a minimal Stop object for unit-test use, where the test only care about id, name,
   * description and coordinate. The feedId is static set to "F"
   */
  public static Stop stopForTest(
    String idAndName,
    String desc,
    double lat,
    double lon,
    Station parent,
    WheelChairBoarding wheelChairBoarding
  ) {
    var stop = new Stop(
      new FeedScopedId("F", idAndName),
      new NonLocalizedString(idAndName),
      idAndName,
      desc,
      new WgsCoordinate(lat, lon),
      wheelChairBoarding,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    stop.setParentStation(parent);
    return stop;
  }

  public void addBoardingArea(BoardingArea boardingArea) {
    if (boardingAreas == null) {
      boardingAreas = new HashSet<>();
    }
    boardingAreas.add(boardingArea);
  }

  @Override
  public String toString() {
    return "<Stop " + getId() + ">";
  }

  public I18NString getUrl() {
    return url;
  }

  public String getPlatformCode() {
    return platformCode;
  }

  public TransitMode getVehicleType() {
    return vehicleType;
  }

  @Override
  public String getVehicleSubmode() {
    return netexSubmode;
  }

  public Collection<FareZone> getFareZones() {
    return Collections.unmodifiableCollection(fareZones);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(getCoordinate().asJtsCoordinate());
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  /**
   * Get the transfer cost priority for Stop. This will fetch the value from the parent [if parent
   * exist] or return the default value.
   */
  @NotNull
  public StopTransferPriority getPriority() {
    return isPartOfStation() ? getParentStation().getPriority() : StopTransferPriority.ALLOWED;
  }

  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas != null ? boardingAreas : Collections.emptySet();
  }
}
