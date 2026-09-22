package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * A trip A -> D on a road going east, with transit stops off it, a walk-only stop T5 behind D, and
 * a far stop F reachable by car but only with a detour no budget allows.
 * <pre>
 *            T1       T3   T5 (walk-only spur off D)
 *   A ------ B ------ C ------ D ------------------ F (50 km)
 *            T2       T4
 * </pre>
 */
class CorridorBuilderTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  private CarpoolStopIndex stopIndex;
  private IntersectionVertex a;
  private IntersectionVertex d;
  private IntersectionVertex iT3;
  private TransitStopVertex t1;
  private TransitStopVertex t3;
  private TransitStopVertex t5;
  private TransitStopVertex far;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new Builder() {
        @Override
        public void build() {
          a = intersection("A", ORIGIN);
          var b = intersection("B", ORIGIN.moveEastMeters(500));
          var c = intersection("C", ORIGIN.moveEastMeters(1500));
          d = intersection("D", ORIGIN.moveEastMeters(2000));
          biStreet(a, b, 500);
          biStreet(b, c, 1000);
          biStreet(c, d, 500);

          var iT1 = intersection("iT1", ORIGIN.moveEastMeters(250).moveNorthMeters(200));
          var iT2 = intersection("iT2", ORIGIN.moveEastMeters(250).moveSouthMeters(200));
          iT3 = intersection("iT3", ORIGIN.moveEastMeters(1750).moveNorthMeters(200));
          var iT4 = intersection("iT4", ORIGIN.moveEastMeters(1750).moveSouthMeters(200));
          biStreet(a, iT1, 320);
          biStreet(b, iT1, 320);
          biStreet(a, iT2, 320);
          biStreet(b, iT2, 320);
          biStreet(c, iT3, 320);
          biStreet(d, iT3, 320);
          biStreet(c, iT4, 320);
          biStreet(d, iT4, 320);
          t1 = stop("T1", iT1.toWgsCoordinate());
          var t2 = stop("T2", iT2.toWgsCoordinate());
          t3 = stop("T3", iT3.toWgsCoordinate());
          var t4 = stop("T4", iT4.toWgsCoordinate());
          biLink(iT1, t1);
          biLink(iT2, t2);
          biLink(iT3, t3);
          biLink(iT4, t4);

          var iT5 = intersection("iT5", ORIGIN.moveEastMeters(2000).moveNorthMeters(200));
          street(
            d,
            iT5,
            200,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          t5 = stop("T5", iT5.toWgsCoordinate());
          biLink(iT5, t5);

          var iF = intersection("F", ORIGIN.moveEastMeters(52_000));
          biStreet(d, iF, 50_000);
          far = stop("F", iF.toWgsCoordinate());
          biLink(iF, far);
        }
      }
    );
    stopIndex = new CarpoolStopIndex(model.graph(), CarReachableVertexSnapper.createDefault());
  }

  @Test
  void corridorHoldsTheLegAndTheStopsADetourCanServe() {
    var corridor = corridor(Duration.ofMinutes(10));

    var leg = corridor.legDurations().getFirst();
    assertTrue(leg.toSeconds() > 60 && leg.toSeconds() < 600, "A -> D is a few minutes: " + leg);
    assertEquals(leg.plus(Duration.ofMinutes(10)), corridor.legLimits().getFirst());
    assertTrue(stopIds(corridor).containsAll(Set.of(t1.getId(), t3.getId(), t5.getId())));
    assertFalse(stopIds(corridor).contains(far.getId()), "F needs a 100 km detour");
  }

  @Test
  void stopTimesAreTheDrivingTimesToAndFromTheStopsCarVertex() {
    var corridor = corridor(Duration.ofMinutes(10));

    var atT3 = corridorStop(corridor, t3);
    assertEquals(0, atT3.leg());
    assertTrue(atT3.servesDropoff() && atT3.servesPickup());
    assertTrue(atT3.dropoffToStopSeconds() > 0 && atT3.dropoffFromStopSeconds() > 0);
    assertTrue(
      atT3.dropoffToStopSeconds() + atT3.dropoffFromStopSeconds() <=
        corridor.legLimits().getFirst().toSeconds()
    );

    // T5 is served at D itself: the whole leg to get there, nothing to drive afterwards.
    var atT5 = corridorStop(corridor, t5);
    assertEquals(corridor.legDurations().getFirst().toSeconds(), atT5.dropoffToStopSeconds());
    assertEquals(0, atT5.dropoffFromStopSeconds());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -5 })
  void withoutABudgetOnlyTheStopsOnTheRouteRemain(int budgetMinutes) {
    var corridor = corridor(Duration.ofMinutes(budgetMinutes));

    assertEquals(corridor.legDurations().getFirst(), corridor.legLimits().getFirst());
    assertTrue(stopIds(corridor).contains(t5.getId()), "T5 is served at D, the end of the route");
    assertFalse(stopIds(corridor).contains(t1.getId()), "T1 needs a detour");
  }

  @Test
  void aTripWhoseLegCannotBeRoutedHasNoCorridor() {
    var unroutable = new CorridorBuilder(stopIndex, (from, to) -> null, 40.0);
    assertNull(unroutable.build(trip(Duration.ofMinutes(10)), List.of(a, d)));
  }

  @Test
  void anotherRequestsTemporaryEdgesAreNotDriven() {
    // Another request's location, linked to both A and D: a free shortcut no car can take.
    var hub = new TemporaryStreetLocation(
      ORIGIN.moveEastMeters(1000).asJtsCoordinate(),
      new NonLocalizedString("hub")
    );
    TemporaryFreeEdge.createTemporaryFreeEdge(a, hub);
    TemporaryFreeEdge.createTemporaryFreeEdge(hub, d);

    var corridor = corridor(Duration.ofMinutes(10));

    var leg = corridor.legDurations().getFirst().toSeconds();
    assertTrue(leg > 60, "A -> D is driven: " + leg);
    assertEquals(leg, corridorStop(corridor, t5).dropoffToStopSeconds());
  }

  @Test
  void ellipseEnvelopeContainsBothFociAndTheMidpointNeighbourhood() {
    var envelope = CorridorBuilder.ellipseEnvelope(d, iT3, 600, 40.0);
    assertTrue(envelope.contains(d.getLon(), d.getLat()));
    assertTrue(envelope.contains(iT3.getLon(), iT3.getLat()));
    // 600 s at 40 m/s is 24 km of driving; the box reaches ~12 km from the midpoint.
    var farNorth = d.toWgsCoordinate().moveNorthMeters(11_000);
    assertTrue(envelope.contains(farNorth.longitude(), farNorth.latitude()));
    var tooFar = d.toWgsCoordinate().moveNorthMeters(14_000);
    assertFalse(envelope.contains(tooFar.longitude(), tooFar.latitude()));
  }

  private CarpoolCorridor corridor(Duration budget) {
    var builder = new CorridorBuilder(stopIndex, StreetLimitationParametersService.DEFAULT);
    return builder.build(trip(budget), List.of(a, d));
  }

  private CarpoolTrip trip(Duration budget) {
    return CarpoolTripTestData.createTripWithDeviationBudget(
      budget,
      a.toWgsCoordinate(),
      d.toWgsCoordinate()
    );
  }

  private static Set<FeedScopedId> stopIds(CarpoolCorridor corridor) {
    return corridor
      .stops()
      .stream()
      .map(CarpoolCorridor.CorridorStop::stopId)
      .collect(Collectors.toSet());
  }

  private static CarpoolCorridor.CorridorStop corridorStop(
    CarpoolCorridor corridor,
    TransitStopVertex t
  ) {
    return corridor
      .stops()
      .stream()
      .filter(s -> s.stopId().equals(t.getId()))
      .findFirst()
      .orElseThrow();
  }
}
