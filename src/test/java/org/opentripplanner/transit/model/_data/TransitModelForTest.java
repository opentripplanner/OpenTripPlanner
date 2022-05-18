package org.opentripplanner.transit.model._data;

import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Test utility class to help construct valid transit model objects.
 */
public class TransitModelForTest {

  public static final String TIME_ZONE_ID = "Europe/Paris";
  public static final String FEED_ID = "F";

  public static Agency agency(String name) {
    return Agency.of(id(name)).setName(name).setTimezone("Europe/Paris").build();
  }

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
