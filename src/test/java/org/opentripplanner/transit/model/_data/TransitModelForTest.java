package org.opentripplanner.transit.model._data;

import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Test utility class to help construct valid transit model objects.
 */
public class TransitModelForTest {

  public static final String TIME_ZONE_ID = "Europe/Paris";
  public static final String FEED_ID = "F";
  public static final Agency AGENCY = Agency
    .of(id("A1"))
    .setName("Agency Test")
    .setTimezone(TIME_ZONE_ID)
    .build();

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  public static Agency agency(String name) {
    return Agency.of(id(name)).setName(name).setTimezone(TIME_ZONE_ID).build();
  }

  /** Create a valid Bus Route to use in unit tests */
  public static RouteBuilder route(String id) {
    return Route.of(id(id)).withAgency(AGENCY).withShortName("R" + id).withMode(TransitMode.BUS);
  }
}
