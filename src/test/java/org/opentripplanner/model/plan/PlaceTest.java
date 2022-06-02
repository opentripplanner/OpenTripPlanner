package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.util.NonLocalizedString;

public class PlaceTest {

  @Test
  public void sameLocationBasedOnInstance() {
    Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
    assertTrue(aPlace.sameLocation(aPlace), "same instance");
  }

  @Test
  public void sameLocationBasedOnCoordinates() {
    Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
    Place samePlace = Place.normal(
      60.000000000001,
      10.0000000000001,
      new NonLocalizedString("Same Place")
    );
    Place otherPlace = Place.normal(65.0, 14.0, new NonLocalizedString("Other Place"));

    assertTrue(aPlace.sameLocation(samePlace), "same place");
    assertTrue(samePlace.sameLocation(aPlace), "same place(symmetric)");
    assertFalse(aPlace.sameLocation(otherPlace), "other place");
    assertFalse(otherPlace.sameLocation(aPlace), "other place(symmetric)");
  }

  @Test
  public void sameLocationBasedOnStopId() {
    var s1 = stop("1", 1.0, 1.0);
    var s2 = stop("2", 1.0, 2.0);

    Place aPlace = place(s1);
    Place samePlace = place(s1);
    Place otherPlace = place(s2);

    assertTrue(aPlace.sameLocation(samePlace), "same place");
    assertTrue(samePlace.sameLocation(aPlace), "same place(symmetric)");
    assertFalse(aPlace.sameLocation(otherPlace), "other place");
    assertFalse(otherPlace.sameLocation(aPlace), "other place(symmetric)");
  }

  @Test
  public void acceptsNullCoordinates() {
    var p = Place.normal(null, null, new NonLocalizedString("Test"));
    assertNull(p.coordinate);
  }

  private static Stop stop(String stopId, double lat, double lon) {
    return new Stop(
      new FeedScopedId("S", stopId),
      new NonLocalizedString(stopId),
      null,
      null,
      WgsCoordinate.creatOptionalCoordinate(lat, lon),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  private static Place place(Stop stop) {
    return Place.forStop(stop);
  }
}
