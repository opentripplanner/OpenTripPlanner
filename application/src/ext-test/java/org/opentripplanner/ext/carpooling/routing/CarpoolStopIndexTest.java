package org.opentripplanner.ext.carpooling.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
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
          var a = intersection("A", ORIGIN);
          var b = intersection("B", ORIGIN.moveEastMeters(500));
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
    index = new CarpoolStopIndex(
      model.graph(),
      new CarReachableVertexSnapper(CarpoolingParameters.DEFAULT.minCarEscapeMeters()),
      CarpoolingParameters.DEFAULT.maxStopWalk()
    );
  }

  @Test
  void indexesEveryStopVertexOfTheGraph() {
    var aroundAllStops = new Envelope(
      ORIGIN.moveWestMeters(100).longitude(),
      ORIGIN.moveEastMeters(1100).longitude(),
      ORIGIN.moveSouthMeters(400).latitude(),
      ORIGIN.moveNorthMeters(50_100).latitude()
    );

    assertThat(index.stopsWithin(aroundAllStops)).containsExactly(t1, t2, t3, t4);
  }

  @Test
  void stopOnAWalkOnlySpurSnapsToTheNearestDrivableVertexWithAWalk() {
    var dropoff = index.dropoffSnap(t3.getId());
    assertNotNull(dropoff);
    assertSame(c, dropoff.vertex());
    assertNotNull(dropoff.walkPath());
    assertTrue(dropoff.walkPath().duration().isPositive());
    assertSame(
      c,
      dropoff.walkPath().states().getFirst().getVertex(),
      "walk runs from the car to the stop"
    );
    assertSame(t3, dropoff.walkPath().lastState().getVertex());

    var pickup = index.pickupSnap(t3.getId());
    assertNotNull(pickup);
    assertSame(c, pickup.vertex());
    assertSame(
      t3,
      pickup.walkPath().states().getFirst().getVertex(),
      "walk runs from the stop to the car"
    );
    assertSame(c, pickup.walkPath().lastState().getVertex());
  }

  @Test
  void unlinkedOrUnknownStopHasNoSnap() {
    assertNull(index.dropoffSnap(t4.getId()));
    assertNull(index.pickupSnap(t4.getId()));
    assertNull(index.dropoffSnap(FeedScopedId.ofNullable("F", "nope")));
  }

  @Test
  void stopsWithinAnEnvelopeIncludeTheStopsAStopWalkAway() {
    var aroundA = new Envelope(
      ORIGIN.moveWestMeters(100).longitude(),
      ORIGIN.moveEastMeters(100).longitude(),
      ORIGIN.moveSouthMeters(100).latitude(),
      ORIGIN.moveNorthMeters(100).latitude()
    );
    var found = index
      .stopsWithin(aroundA)
      .stream()
      .map(TransitStopVertex::getId)
      .collect(Collectors.toSet());

    // T2 and T3 lie 0.5 and 1 km east, within the 15-minute stop walk; T4 lies 50 km north.
    assertThat(found).containsExactly(t1.getId(), t2.getId(), t3.getId());
    assertThat(index.stopsWithin(new Envelope(0, 1, 0, 1))).isEmpty();
  }
}
