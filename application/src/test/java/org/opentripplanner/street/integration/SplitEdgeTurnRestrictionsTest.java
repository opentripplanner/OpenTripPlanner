package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.test.support.ResourceLoader;

/*
 * When bus stops are added to graph they split an existing edge in two parts so that an artificial
 * intersection can be added and routes can be found to the station.
 *
 * During this process turn restrictions also need to be propagated from the old edges to the newly
 * created ones. This test makes sure that this happens correctly.
 */
public class SplitEdgeTurnRestrictionsTest {

  static final Instant dateTime = LocalDateTime.of(2020, 3, 3, 7, 0)
    .atZone(ZoneIds.BERLIN)
    .toInstant();
  // Deufringen
  static final GenericLocation hardtheimerWeg = new GenericLocation(48.67765, 8.87212);
  static final GenericLocation steinhaldenWeg = new GenericLocation(48.67815, 8.87305);
  static final GenericLocation k1022 = new GenericLocation(48.67846, 8.87021);
  // Böblingen
  static final GenericLocation paulGerhardtWegEast = new GenericLocation(48.68363, 9.00728);
  static final GenericLocation paulGerhardtWegWest = new GenericLocation(48.68297, 9.00520);
  static final GenericLocation parkStrasse = new GenericLocation(48.68358, 9.00826);
  static final GenericLocation herrenbergerStrasse = new GenericLocation(48.68497, 9.00909);
  static final GenericLocation steinbeissWeg = new GenericLocation(48.68172, 9.00599);
  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(
    SplitEdgeTurnRestrictionsTest.class
  );

  @Test
  void shouldTakeDeufringenTurnRestrictionsIntoAccount() {
    TestOtpModel model = ConstantsForTests.buildOsmAndGtfsGraph(
      RESOURCE_LOADER.file("deufringen-minimal.osm.pbf"),
      RESOURCE_LOADER.file("vvs-bus-764-only.gtfs.zip")
    );
    Graph graph = model.graph();
    // https://www.openstreetmap.org/relation/10264251 has a turn restriction so when leaving Hardtheimer Weg
    // you must either turn right and take the long way to Steinhaldenweg or go past the intersection with the
    // turn restriction and turn around.
    // on top of this, it has a bus stop so this test also makes sure that the turn restrictions work
    // even when the streets are split.
    var noRightTurnPermitted = computeCarPolyline(graph, hardtheimerWeg, steinhaldenWeg);
    assertThatPolylinesAreEqual(
      noRightTurnPermitted,
      "ijbhHuycu@g@Uq@[e@|BMf@YxA]xAYz@Yp@Yj@^n@JDN_@?Wa@i@Xq@X{@\\yAXyACGAIB]j@_DPaA@e@MDCB"
    );

    // when to drive in reverse direction it's fine to go this way
    var leftTurnOk = computeCarPolyline(graph, steinhaldenWeg, hardtheimerWeg);
    assertThatPolylinesAreEqual(leftTurnOk, "kmbhHo_du@BCLEAd@Q`Ak@~CC\\@HBFFWDOd@}Bp@Zf@T");

    // make sure that going straight on a straight-only turn direction also works
    var straightAhead = computeCarPolyline(graph, hardtheimerWeg, k1022);
    assertThatPolylinesAreEqual(straightAhead, "ijbhHuycu@g@Uq@[e@|BMf@YxA]xAXn@Hd@");

    var straightAheadBack = computeCarPolyline(graph, k1022, hardtheimerWeg);
    assertThatPolylinesAreEqual(straightAheadBack, "kobhHwmcu@Ie@Yo@\\yAXyAFWDOd@}Bp@Zf@T");

    // make sure that turning left onto the minor road works even when the opposite direction has a straight-only
    // restriction
    var leftTurnAllowed = computeCarPolyline(graph, k1022, steinhaldenWeg);
    assertThatPolylinesAreEqual(leftTurnAllowed, "kobhHwmcu@Ie@Yo@\\yAXyACGAIB]j@_DPaA@e@MDCB");

    var rightTurnAllowed = computeCarPolyline(graph, steinhaldenWeg, k1022);
    assertThatPolylinesAreEqual(rightTurnAllowed, "kmbhHo_du@BCLEAd@Q`Ak@~CC\\@HBFYxA]xAXn@Hd@");
  }

  @Test
  void shouldTakeBoeblingenTurnRestrictionsIntoAccount() {
    // this tests that the following turn restriction is transferred correctly to the split edges
    // https://www.openstreetmap.org/relation/299171
    TestOtpModel model = ConstantsForTests.buildOsmAndGtfsGraph(
      RESOURCE_LOADER.file("boeblingen-minimal.osm.pbf"),
      RESOURCE_LOADER.file("vvs-bus-751-only.gtfs.zip")
    );
    var graph = model.graph();

    // turning left from the main road onto a residential one
    var turnLeft = computeCarPolyline(graph, parkStrasse, paulGerhardtWegEast);
    assertThatPolylinesAreEqual(
      turnLeft,
      "kochHsl~u@HQL]N_@v@mBDKN]KKM\\{@~BKXWj@KRKPCFYj@DP^lAJX"
    );

    // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
    // is a bus station along the way, splitting the edge
    var noLeftTurnPermitted = computeCarPolyline(graph, paulGerhardtWegEast, parkStrasse);
    assertThatPolylinesAreEqual(noLeftTurnPermitted, "sochHof~u@KY_@mAVi@Te@DK");

    // right hand turn out of the the residential road onto the main road, only right turn allowed plus there
    // is a bus station along the way, splitting the edge
    var longWay = computeCarPolyline(graph, paulGerhardtWegEast, herrenbergerStrasse);
    assertThatPolylinesAreEqual(
      longWay,
      "sochHof~u@KY_@mAVi@Te@N]L]N_@v@mBDKN]KKM\\{@~BKXWj@KRKPCFa@`@_@XWPSHQDMCEAQMKKSgAa@qCMe@"
    );

    var longWayBack = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegEast);
    assertThatPolylinesAreEqual(
      longWayBack,
      "axchHwq~u@G_@Qc@@UCMAK@Q@WTUh@eA@Cb@gANg@Nu@Lq@Fe@Da@Bo@Bq@BUD[Je@Li@DWFBHJt@bAFFTZLN@@d@j@|@lA`@r@\\r@z@tBLZ]TYX]`@e@z@Yp@GJM\\{@~BKXWj@KRKPCFYj@DP^lAJX"
    );

    // test that you can correctly turn right here https://www.openstreetmap.org/relation/415123 when approaching
    // from south
    var fromSouth = computeCarPolyline(graph, steinbeissWeg, paulGerhardtWegWest);
    assertThatPolylinesAreEqual(fromSouth, "wcchHk~}u@Fd@Hj@o@\\{@b@KFyBlAWmA");
    var toSouth = computeCarPolyline(graph, paulGerhardtWegWest, steinbeissWeg);
    assertThatPolylinesAreEqual(toSouth, "okchHoy}u@VlAxBmAJGz@c@n@]Ik@Ge@");

    // test that you cannot turn left here https://www.openstreetmap.org/relation/415123 when approaching
    // from north
    var fromNorth = computeCarPolyline(graph, paulGerhardtWegWest, herrenbergerStrasse);
    assertThatPolylinesAreEqual(
      fromNorth,
      "okchHoy}u@VlA{BlAIBOLCBIDc@{AYiAM_@Kc@K_@I_@Ia@Ga@Gc@Gc@Ei@EYAIKaAEe@CQCSIm@SgAa@qCMe@"
    );

    // when you approach you cannot turn left so you have to take a long way
    var toNorth = computeCarPolyline(graph, herrenbergerStrasse, paulGerhardtWegWest);
    assertThatPolylinesAreEqual(
      toNorth,
      "axchHwq~u@G_@Qc@@UCMAK@Q@WTUh@eA@Cb@gANg@Nu@Lq@Fe@Da@Bo@Bq@BUD[Je@Li@DWFBHJt@bAFFTZLN@@d@j@|@lA`@r@\\r@z@tBLZ]TYX]`@e@z@Yp@GJJJBDDDBBBFvAhCXv@Rp@`@lC@Ff@`D@HRpAJt@Hj@o@\\{@b@KFyBlAWmA"
    );
  }

  private static String computeCarPolyline(Graph graph, GenericLocation from, GenericLocation to) {
    RouteRequest request = new RouteRequest();
    request.setDateTime(dateTime);
    request.setFrom(from);
    request.setTo(to);

    request.journey().direct().setMode(StreetMode.CAR);
    var temporaryVertices = new TemporaryVerticesContainer(
      graph,
      from,
      to,
      StreetMode.CAR,
      StreetMode.CAR
    );
    var gpf = new GraphPathFinder(null);
    var paths = gpf.graphPathFinderEntryPoint(request, temporaryVertices);

    GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      ZoneIds.BERLIN,
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    var itineraries = graphPathToItineraryMapper.mapItineraries(paths);
    temporaryVertices.close();

    // make sure that we only get CAR legs
    itineraries.forEach(i ->
      i
        .legs()
        .forEach(l -> {
          if (l instanceof StreetLeg stLeg) {
            assertEquals(TraverseMode.CAR, stLeg.getMode());
          } else {
            fail("Expected StreetLeg (CAR): " + l);
          }
        })
    );
    Geometry geometry = itineraries.get(0).legs().get(0).legGeometry();
    return EncodedPolyline.encode(geometry).points();
  }
}
