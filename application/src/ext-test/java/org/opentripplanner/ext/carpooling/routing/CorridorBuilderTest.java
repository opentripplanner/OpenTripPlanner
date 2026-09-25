package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * Same layout as the access/egress service test: a road A - B - C - D going east with transit
 * stops off it, a walk-only stop T5 behind D, and a far stop F reachable by car but only with a
 * detour no budget allows.
 * <pre>
 *            T1       T3   T5 (walk-only spur off D)
 *   A ------ B ------ C ------ D ------------------ F (50 km)
 *            T2       T4
 * </pre>
 */
class CorridorBuilderTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);
  private static final ZonedDateTime TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZoneId.of("Europe/Oslo")
  );

  private CarpoolStopIndex stopIndex;
  private CarReachableVertexSnapper snapper;
  private VertexCreationService vertexCreationService;
  private CarpoolTripVertexResolver resolver;
  private IntersectionVertex d;
  private IntersectionVertex iT3;
  private TransitStopVertex t1;
  private TransitStopVertex t3;
  private TransitStopVertex t5;
  private TransitStopVertex far;
  private WgsCoordinate coordA;
  private WgsCoordinate coordD;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new Builder() {
        @Override
        public void build() {
          var a = intersection("A", ORIGIN);
          var b = intersection("B", ORIGIN.moveEastMeters(500));
          var c = intersection("C", ORIGIN.moveEastMeters(1500));
          d = intersection("D", ORIGIN.moveEastMeters(2000));
          coordA = a.toWgsCoordinate();
          coordD = d.toWgsCoordinate();
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
    snapper = CarReachableVertexSnapper.createDefault();
    stopIndex = new CarpoolStopIndex(
      model.graph(),
      snapper,
      CarpoolingParameters.DEFAULT.maxStopWalk()
    );
    vertexCreationService = new VertexCreationService(VertexLinkerTestFactory.of(model.graph()));
    resolver = new CarpoolTripVertexResolver(
      vertexCreationService,
      snapper,
      new CorridorBuilder(stopIndex, StreetLimitationParametersService.DEFAULT)
    );
  }

  @Test
  void corridorHoldsTheBaselineAndTheStopsInsideTheLegsEllipse() {
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, TIME);
    var resolved = resolver.resolve(trip);
    assertNotNull(resolved);
    var corridor = resolved.corridor();
    assertNotNull(corridor, "the resolver attaches the corridor");

    assertEquals(1, corridor.legCount());
    var legSeconds = corridor.legDurations().getFirst().toSeconds();
    assertTrue(legSeconds > 60 && legSeconds < 600, "A -> D is a few minutes: " + legSeconds);
    assertEquals(
      DriverLegLimits.legLimits(trip, new Duration[] { corridor.legDurations().getFirst() })[0],
      corridor.legLimits().getFirst()
    );

    var ids = corridor
      .stops()
      .stream()
      .map(s -> s.stopId())
      .collect(Collectors.toSet());
    assertTrue(ids.contains(t1.getId()), "T1 is inside the ellipse");
    assertTrue(ids.contains(t3.getId()), "T3 is inside the ellipse");
    assertTrue(ids.contains(t5.getId()), "T5 is reached by walking from D");
    assertFalse(ids.contains(far.getId()), "F needs a 100 km detour: outside every budget");
    assertEquals(1, corridor.legEnvelopes().size());
    assertTrue(corridor.legEnvelopes().getFirst().contains(coordA.longitude(), coordA.latitude()));
  }

  @Test
  void stopTimesAreTheDrivingTimesToAndFromTheSnapVertex() {
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, TIME);
    var corridor = resolver.resolve(trip).corridor();
    assertNotNull(corridor);
    long bound = corridor.legLimits().getFirst().toSeconds();

    var t3Entry = corridor
      .stops()
      .stream()
      .filter(s -> s.stopId().equals(t3.getId()))
      .findFirst()
      .orElseThrow();
    assertTrue(t3Entry.servesDropoff());
    assertTrue(t3Entry.servesPickup());
    assertTrue(t3Entry.dropoffToStopSeconds() > 0, "A -> iT3 takes time");
    assertTrue(t3Entry.dropoffFromStopSeconds() > 0, "iT3 -> D takes time");
    assertTrue(t3Entry.dropoffToStopSeconds() + t3Entry.dropoffFromStopSeconds() <= bound);
    assertEquals(0, t3Entry.leg());

    // T5's snap is D itself: the whole leg to get there, nothing left to drive afterwards.
    var t5Entry = corridor
      .stops()
      .stream()
      .filter(s -> s.stopId().equals(t5.getId()))
      .findFirst()
      .orElseThrow();
    assertTrue(t5Entry.servesDropoff());
    assertEquals(corridor.legDurations().getFirst().toSeconds(), t5Entry.dropoffToStopSeconds());
    assertEquals(0, t5Entry.dropoffFromStopSeconds());
  }

  @Test
  void unroutableBaselineYieldsNoCorridor() {
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, TIME);
    var withoutBuilder = new CarpoolTripVertexResolver(vertexCreationService, snapper).resolve(
      trip
    );
    assertNotNull(withoutBuilder);
    assertNull(withoutBuilder.corridor(), "no builder, no corridor");

    CarpoolRouter routerThatFails = (from, to) -> null;
    var failing = new CorridorBuilder(stopIndex, routerThatFails, 40.0);
    assertNull(failing.build(withoutBuilder));
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
}
