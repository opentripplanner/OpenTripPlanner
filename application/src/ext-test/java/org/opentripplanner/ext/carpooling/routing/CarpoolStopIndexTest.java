package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

/**
 * <pre>
 *   A ------ B ------ C          drivable streets
 *   |        |        :
 *   T1       T2       :  pedestrian street C - W
 *                     W
 *                     |
 *                     T3        (stop on a walk-only spur)
 *
 *   T4  unlinked stop, far away
 * </pre>
 */
class CarpoolStopIndexTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  private CarpoolStopIndex index;
  private IntersectionVertex a;
  private IntersectionVertex b;
  private IntersectionVertex c;
  private TransitStopVertex t1;
  private TransitStopVertex t2;
  private TransitStopVertex t3;
  private TransitStopVertex t4;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new Builder() {
        @Override
        public void build() {
          a = intersection("A", ORIGIN);
          b = intersection("B", ORIGIN.moveEastMeters(500));
          c = intersection("C", ORIGIN.moveEastMeters(1000));
          biStreet(a, b, 500);
          biStreet(b, c, 500);

          t1 = stop("T1", a.toWgsCoordinate().moveSouthMeters(20));
          t2 = stop("T2", b.toWgsCoordinate().moveSouthMeters(20));
          biLink(a, t1);
          biLink(b, t2);

          var w = intersection("W", ORIGIN.moveEastMeters(1000).moveSouthMeters(300));
          street(
            c,
            w,
            300,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          t3 = stop("T3", w.toWgsCoordinate().moveSouthMeters(20));
          biLink(w, t3);

          t4 = stop("T4", ORIGIN.moveNorthMeters(50_000));
        }
      }
    );
    index = new CarpoolStopIndex(model.graph(), CarReachableVertexSnapper.createDefault());
  }

  @Test
  void indexesEveryStopVertexOfTheGraph() {
    assertEquals(4, index.size());
    assertNotNull(index.dropoffSnap(t1.getId()));
    assertNull(
      index.dropoffSnap(org.opentripplanner.core.model.id.FeedScopedId.ofNullable("F", "nope"))
    );
  }

  @Test
  void stopOnADrivableStreetSnapsToItsLinkVertexWithoutWalking() {
    var snap = index.dropoffSnap(t1.getId());
    assertNotNull(snap);
    assertSame(a, snap.vertex());
    assertNull(snap.walkPath());

    var pickup = index.pickupSnap(t1.getId());
    assertNotNull(pickup);
    assertSame(a, pickup.vertex());
  }

  @Test
  void stopOnAWalkOnlySpurSnapsToTheNearestDrivableVertexWithAWalk() {
    var dropoff = index.dropoffSnap(t3.getId());
    assertNotNull(dropoff);
    assertSame(c, dropoff.vertex());
    assertNotNull(dropoff.walkPath());
    assertTrue(dropoff.walkPath().getDuration() > 0);
    assertSame(
      c,
      dropoff.walkPath().states.getFirst().getVertex(),
      "walk runs from the car to the stop"
    );
    assertSame(t3, dropoff.walkPath().states.getLast().getVertex());

    var pickup = index.pickupSnap(t3.getId());
    assertNotNull(pickup);
    assertSame(c, pickup.vertex());
    assertSame(
      t3,
      pickup.walkPath().states.getFirst().getVertex(),
      "walk runs from the stop to the car"
    );
    assertSame(c, pickup.walkPath().states.getLast().getVertex());
  }

  @Test
  void unlinkedStopHasNoSnap() {
    assertNull(index.dropoffSnap(t4.getId()));
    assertNull(index.pickupSnap(t4.getId()));
  }

  @Test
  void snapsAreComputedOnceAndShared() {
    assertSame(index.dropoffSnap(t3.getId()), index.dropoffSnap(t3.getId()));
  }

  @Test
  void stopsWithinAnEnvelopeComeFromTheGrid() {
    var aroundAAndB = new Envelope(
      ORIGIN.moveWestMeters(100).longitude(),
      ORIGIN.moveEastMeters(600).longitude(),
      ORIGIN.moveSouthMeters(100).latitude(),
      ORIGIN.moveNorthMeters(100).latitude()
    );
    var found = index
      .stopsWithin(aroundAAndB)
      .stream()
      .map(s -> s.getId())
      .collect(Collectors.toSet());
    assertEquals(Set.of(t1.getId(), t2.getId()), found);

    var everything = new Envelope(9.0, 12.0, 62.0, 65.0);
    assertEquals(4, index.stopsWithin(everything).size());
    assertTrue(index.stopsWithin(new Envelope(0, 1, 0, 1)).isEmpty());
  }
}
