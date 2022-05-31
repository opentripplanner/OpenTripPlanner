package org.opentripplanner.transit.model._data;

import static org.opentripplanner.model.WheelchairAccessibility.NO_INFORMATION;

import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTransferPriority;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Test utility class to help construct valid transit model objects.
 */
public class TransitModelForTest {

  public static final String FEED_ID = "F";
  public static final String TIME_ZONE_ID = "Europe/Paris";
  public static final Agency AGENCY = Agency
    .of(id("A1"))
    .withName("Agency Test")
    .withTimezone(TIME_ZONE_ID)
    .withUrl("https://www.agency.com")
    .build();

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  public static Agency agency(String name) {
    return AGENCY.copy().withId(id(name)).withName(name).build();
  }

  /** Create a valid Bus Route to use in unit tests */
  public static RouteBuilder route(String id) {
    return Route.of(id(id)).withAgency(AGENCY).withShortName("R" + id).withMode(TransitMode.BUS);
  }

  /** Create a valid Bus Route to use in unit tests */
  public static TripBuilder trip(String id) {
    return Trip.of(id(id)).withRoute(route("R" + id).build());
  }

  public static Stop stopForTest(
    String idAndName,
    WheelchairAccessibility wheelchair,
    double lat,
    double lon
  ) {
    return stopForTest(idAndName, null, lat, lon, null, wheelchair);
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
    WheelchairAccessibility wheelchair
  ) {
    var stop = new Stop(
      new FeedScopedId("F", idAndName),
      new NonLocalizedString(idAndName),
      idAndName,
      NonLocalizedString.ofNullable(desc),
      new WgsCoordinate(lat, lon),
      wheelchair,
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

  /**
   * Create a minimal Station object for unit-test use, where the test only care about id, name and
   * coordinate. The feedId is static set to "F"
   */
  public static Station stationForTest(String idAndName, double lat, double lon) {
    return new Station(
      new FeedScopedId("F", idAndName),
      new NonLocalizedString(idAndName),
      new WgsCoordinate(lat, lon),
      idAndName,
      new NonLocalizedString("Station " + idAndName),
      null,
      null,
      StopTransferPriority.ALLOWED
    );
  }
}
