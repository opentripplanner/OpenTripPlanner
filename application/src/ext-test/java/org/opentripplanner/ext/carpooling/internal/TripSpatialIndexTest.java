package org.opentripplanner.ext.carpooling.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;

class TripSpatialIndexTest {

  private static final FeedScopedId T1 = FeedScopedId.ofNullable("F", "t1");
  private static final FeedScopedId T2 = FeedScopedId.ofNullable("F", "t2");

  @Test
  void findsTripsWhoseEnvelopesCoverThePoint() {
    var index = new TripSpatialIndex();
    // Trondheim area: lon 10.2-10.6, lat 63.3-63.5
    index.put(T1, List.of(new Envelope(10.2, 10.6, 63.3, 63.5)));
    // Steinkjer area
    index.put(T2, List.of(new Envelope(11.3, 11.7, 63.9, 64.1)));

    assertEquals(Set.of(T1), index.near(63.43, 10.39));
    assertEquals(Set.of(T2), index.near(64.0, 11.5));
    assertTrue(index.near(60.0, 10.0).isEmpty());
    assertEquals(2, index.size());
  }

  @Test
  void aTripIsRegisteredUnderAllCellsOfAllItsLegs() {
    var index = new TripSpatialIndex();
    index.put(
      T1,
      List.of(new Envelope(10.2, 10.3, 63.3, 63.35), new Envelope(11.3, 11.4, 63.9, 63.95))
    );

    assertEquals(Set.of(T1), index.near(63.32, 10.25));
    assertEquals(Set.of(T1), index.near(63.92, 11.35));
    assertTrue(index.near(63.6, 10.8).isEmpty(), "the gap between the legs is not covered");
  }

  @Test
  void reRegisteringReplacesAndRemovingForgets() {
    var index = new TripSpatialIndex();
    index.put(T1, List.of(new Envelope(10.2, 10.6, 63.3, 63.5)));
    index.put(T1, List.of(new Envelope(11.3, 11.7, 63.9, 64.1)));
    assertTrue(index.near(63.43, 10.39).isEmpty(), "the old cells are released");
    assertEquals(Set.of(T1), index.near(64.0, 11.5));

    index.remove(T1);
    assertTrue(index.near(64.0, 11.5).isEmpty());
    assertEquals(0, index.size());
    index.remove(T1);
  }

  @Test
  void neighbouringCellsAreNotReturned() {
    var index = new TripSpatialIndex();
    // Exactly one cell: [63.30, 63.35) x [10.20, 10.25)
    index.put(T1, List.of(new Envelope(10.21, 10.24, 63.31, 63.34)));
    assertEquals(Set.of(T1), index.near(63.32, 10.22));
    assertTrue(index.near(63.32, 10.27).isEmpty());
    assertTrue(index.near(63.36, 10.22).isEmpty());
  }
}
