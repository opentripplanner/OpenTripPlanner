package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.test.support.ResourceLoader;

public class CarRoutingTest {

  static final Instant dateTime = Instant.now();
  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(CarRoutingTest.class);

  private static Graph herrenbergGraph;

  @BeforeAll
  public static void setup() {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      RESOURCE_LOADER.file("herrenberg-minimal.osm.pbf")
    );
    herrenbergGraph = model.index().graph();
  }

  /**
   * The OTP algorithm tries hard to never visit the same node twice. This is generally a good idea
   * because it avoids useless loops in the traversal leading to way faster processing time.
   * <p>
   * However there is are certain rare pathological cases where through a series of turn
   * restrictions and roadworks you absolutely must visit a vertex twice if you want to produce a
   * result. One example would be a route like this: https://tinyurl.com/ycqux93g (Note: At the time
   * of writing this Hindenburgstr. (https://www.openstreetmap.org/way/415545869) is closed due to
   * roadworks.)
   * <p>
   * This test checks that such a loop is possible.
   * <p>
   * More information: https://github.com/opentripplanner/OpenTripPlanner/issues/3393
   */
  @Test
  @DisplayName("car routes can contain loops (traversing the same edge twice)")
  public void shouldAllowLoopCausedByTurnRestrictions() {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      RESOURCE_LOADER.file("herrenberg-hindenburgstr-under-construction.osm.pbf")
    );
    var hindenburgStrUnderConstruction = model.index().graph();

    var gueltsteinerStr = GenericLocation.fromCoordinate(48.59386, 8.87088);
    var aufDemGraben = GenericLocation.fromCoordinate(48.59487, 8.87133);

    var polyline = computePolyline(hindenburgStrUnderConstruction, gueltsteinerStr, aufDemGraben);

    assertThatPolylinesAreEqual(
      polyline,
      "s~qgH}qcu@[MuAs@[SAm@Ee@AUEi@XEQkBQ?Bz@Dt@Dh@@TGBC@KBSHGx@"
    );
  }

  @Test
  public void shouldRespectGeneralNoThroughTraffic() {
    var mozartStr = GenericLocation.fromCoordinate(48.59521, 8.88391);
    var fritzLeharStr = GenericLocation.fromCoordinate(48.59460, 8.88291);

    var polyline1 = computePolyline(herrenbergGraph, mozartStr, fritzLeharStr);
    assertThatPolylinesAreEqual(polyline1, "_grgHkcfu@OjBC\\ARGjAKzAfBz@j@n@Rk@E}D");

    var polyline2 = computePolyline(herrenbergGraph, fritzLeharStr, mozartStr);
    assertThatPolylinesAreEqual(polyline2, "gcrgHc}eu@D|DSj@k@o@gB{@J{AFkA@SB]NkB");
  }

  /**
   * Tests that that https://www.openstreetmap.org/way/35097400 is not taken due to
   * motor_vehicle=destination.
   */
  @Test
  public void shouldRespectMotorCarNoThru() {
    var schiessmauer = GenericLocation.fromCoordinate(48.59737, 8.86350);
    var zeppelinStr = GenericLocation.fromCoordinate(48.59972, 8.86239);

    var polyline1 = computePolyline(herrenbergGraph, schiessmauer, zeppelinStr);
    assertThatPolylinesAreEqual(
      polyline1,
      "otrgH{cbu@v@|D?bAElBEv@Cj@APGAY?YD]Fm@X_@Pw@d@eAn@k@VM@]He@Fo@Bi@??c@?Q@gD?Q?Q@mD?S"
    );

    var polyline2 = computePolyline(herrenbergGraph, zeppelinStr, schiessmauer);
    assertThatPolylinesAreEqual(
      polyline2,
      "ccsgH{|au@?RAlD?P?PAfD?P?b@h@?n@Cd@G\\ILAj@WdAo@v@e@^Ql@Y\\GXEX?F@@QBk@Dw@DmB?cAw@}D"
    );
  }

  @Test
  public void planningFromNoThroughTrafficPlaceTest() {
    var noThroughTrafficPlace = GenericLocation.fromCoordinate(48.59634, 8.87020);
    var destination = GenericLocation.fromCoordinate(48.59463, 8.87218);

    var polyline1 = computePolyline(herrenbergGraph, noThroughTrafficPlace, destination);
    assertThatPolylinesAreEqual(
      polyline1,
      "corgHkncu@OEYUOMH?J?LINMNMHTDO@YMm@HS`A}BPGRWLYDEt@HJ@b@?Fc@DONm@t@OXCBz@B\\"
    );

    var polyline2 = computePolyline(herrenbergGraph, destination, noThroughTrafficPlace);
    assertThatPolylinesAreEqual(
      polyline2,
      "scrgH_zcu@C]C{@YBu@NOl@ENGb@c@?KAu@IEDMXSVQFaA|BIRLl@AXENIUOLOLMHK?I?NLXTND"
    );
  }

  private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
    RouteRequest request = RouteRequest.of()
      .withDateTime(dateTime)
      .withFrom(from)
      .withTo(to)
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.CAR)))
      .buildRequest();
    var temporaryVertices = new TemporaryVerticesContainer(
      graph,
      TestVertexLinker.of(graph),
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
    Geometry legGeometry = itineraries.get(0).legs().get(0).legGeometry();
    return EncodedPolyline.encode(legGeometry).points();
  }
}
