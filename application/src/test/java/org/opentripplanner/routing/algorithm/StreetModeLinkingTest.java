package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_PICKUP;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;

/**
 * This tests linking of GenericLocations to streets for each StreetMode. The test has 5 parallel
 * streets and a linking is performed for each mode in the middle of each street. Currently linking
 * is handled in three different ways:
 * <ul>
 *   <li>CAR - TODO : short hint on business rule we want to enforce in test </li>
 *   <li>CAR_PARK - TODO : short hint on business rule we want to enforce in test </li>
 *   <li>everything else - TODO : short hint on business rule we want to enforce in test </li>
 * </ul>
 */
public class StreetModeLinkingTest extends GraphRoutingTest {

  /** 
   * This is used to make parallel streets by adding the value to the longitude.
   * The value make streets about 11m apart. 
   */
  private static final double STREET_DELTA = 0.0001;

  /** The offset is used to place coordinates, close to a line(street) - but not directly on it. */
  private static final double OFFSET = STREET_DELTA / 10.0;

  private static final double LONGITUDE_0 = 20.0000;

  // Make the start and end of each line about 300m apart
  private static final double LATITUDE_START = 47.0050;
  private static final double LATITUDE_END = LATITUDE_START + 0.0027;

  private static final double LATITUDE_MIDDLE = (LATITUDE_START + LATITUDE_END) / 2;

  // Place stop/from/to locations 1m above the first street longitude.
  private static final double LONGITUDE_LOCATION = LONGITUDE_0 - OFFSET;

  // Make parallel streets ~10 meters apart for each street traversal permission
  private static int testCaseIndex = 0;
  private static final StreetTC CAR_TC = StreetTC.of(StreetTraversalPermission.CAR);
  private static final StreetTC ALL_TC = StreetTC.of(StreetTraversalPermission.ALL);
  private static final StreetTC PEDESTRIAN_TC = StreetTC.of(StreetTraversalPermission.PEDESTRIAN);
  private static final StreetTC PEDESTRIAN_BICYCLE_TC = StreetTC.of(
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
  );
  private static final StreetTC BICYCLE_CAR_TC = StreetTC.of(
    StreetTraversalPermission.BICYCLE_AND_CAR
  );

  /**
   * A place used as dummy to/from, when testing from/to. It can be anywhere, except
   * the same location as the place under test.
   */
  private static final GenericLocation ANY_PLACE = new GenericLocation(
    "Any place - not used",
    null,
    LATITUDE_START,
    LONGITUDE_0
  );

  private Graph graph;
  private TransitStopVertex stop;
  private GenericLocation stopLocation;

  @BeforeEach
  protected void setUp() throws Exception {
    // Place stop in the middle of the lines(LATITUDE), and slightly above the first line

    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          CAR_TC.streetEdge(this, false);
          ALL_TC.streetEdge(this, false);
          PEDESTRIAN_TC.streetEdge(this, true);
          PEDESTRIAN_BICYCLE_TC.streetEdge(this, false);
          BICYCLE_CAR_TC.streetEdge(this, false);
          stop = stop("STOP", LATITUDE_MIDDLE, LONGITUDE_LOCATION);
        }
      }
    );
    graph = otpModel.graph();

    graph.hasStreets = true;
    TestStreetLinkerModule.link(graph, otpModel.timetableRepository());
    this.stopLocation = new GenericLocation(stop.getLabelString(), stop.getId(), null, null);
  }

  private static List<Arguments> testPedestrianLinkingTestCases() {
    var args = new ArrayList<Arguments>();
    var modes = List.of(
      WALK,
      BIKE,
      BIKE_TO_PARK,
      BIKE_RENTAL,
      SCOOTER_RENTAL,
      FLEXIBLE,
      CAR_PICKUP,
      CAR_RENTAL
    );

    for (StreetMode mode : modes) {
      args.addAll(
        List.of(
          Arguments.of(mode, CAR_TC, ALL_TC),
          Arguments.of(mode, ALL_TC, ALL_TC),
          Arguments.of(mode, PEDESTRIAN_TC, PEDESTRIAN_TC),
          Arguments.of(mode, PEDESTRIAN_BICYCLE_TC, PEDESTRIAN_BICYCLE_TC),
          Arguments.of(mode, BICYCLE_CAR_TC, PEDESTRIAN_BICYCLE_TC)
        )
      );
    }
    return args;
  }

  /**
   * Only CAR linking is handled specially, since walking with a bike is always a possibility,
   * and so no difference is made between BIKE/WALK:
   */
  @ParameterizedTest
  @MethodSource("testPedestrianLinkingTestCases")
  public void testPedestrianLinking(
    StreetMode mode,
    StreetTC placeCloseToStreetTestCase,
    StreetTC expectedStreet
  ) {
    testLinking(placeCloseToStreetTestCase, expectedStreet, mode);
  }

  @Test
  public void testCarLinking() {
    testLinking(CAR_TC, CAR_TC, CAR);
    testLinking(ALL_TC, ALL_TC, CAR);
    testLinking(PEDESTRIAN_TC, ALL_TC, CAR);
    testLinking(PEDESTRIAN_BICYCLE_TC, BICYCLE_CAR_TC, CAR);
    testLinking(BICYCLE_CAR_TC, BICYCLE_CAR_TC, CAR);
    assertLinkedLocation(stopLocation, CAR_TC, CAR_TC, CAR);
  }

  @Test
  public void testCarParkLinking() {
    testLinking(CAR_TC, CAR_TC, ALL_TC, CAR_TO_PARK);
    testLinking(ALL_TC, ALL_TC, ALL_TC, CAR_TO_PARK);
    testLinking(PEDESTRIAN_TC, ALL_TC, PEDESTRIAN_TC, CAR_TO_PARK);
    testLinking(PEDESTRIAN_BICYCLE_TC, BICYCLE_CAR_TC, PEDESTRIAN_BICYCLE_TC, CAR_TO_PARK);
    testLinking(BICYCLE_CAR_TC, BICYCLE_CAR_TC, PEDESTRIAN_BICYCLE_TC, CAR_TO_PARK);
    assertLinkedLocation(stopLocation, CAR_TC, ALL_TC, CAR_TO_PARK);
  }

  // TODO: Linking to wheelchair accessible streets is currently not implemented,
  //       is this relevant?

  private void testLinking(
    StreetTC linkPlaceOnStreet,
    StreetTC expectedLinkedStreet,
    StreetMode... streetModes
  ) {
    assertLinkedLocation(
      linkPlaceOnStreet.placeCloseToStreet(),
      expectedLinkedStreet,
      expectedLinkedStreet,
      streetModes
    );
  }

  private void testLinking(
    StreetTC linkPlaceOnStreet,
    StreetTC expectedFromStreetName,
    StreetTC expectedToStreetName,
    StreetMode... streetModes
  ) {
    assertLinkedLocation(
      linkPlaceOnStreet.placeCloseToStreet(),
      expectedFromStreetName,
      expectedToStreetName,
      streetModes
    );
  }

  private void assertLinkedLocation(
    GenericLocation location,
    StreetTC expectedFromStreetName,
    StreetTC expectedToStreetName,
    StreetMode... streetModes
  ) {
    for (final StreetMode streetMode : streetModes) {
      try (
        var temporaryVertices = new TemporaryVerticesContainer(
          graph,
          location,
          ANY_PLACE,
          streetMode,
          streetMode
        )
      ) {
        assertFromLink(
          expectedFromStreetName.name(),
          streetMode,
          temporaryVertices.getFromVertices().iterator().next()
        );
      }

      try (
        var temporaryVertices = new TemporaryVerticesContainer(
          graph,
          ANY_PLACE,
          location,
          streetMode,
          streetMode
        )
      ) {
        if (expectedToStreetName != null) {
          assertToLink(
            expectedToStreetName.name(),
            streetMode,
            temporaryVertices.getToVertices().iterator().next()
          );
        }
      }
    }
  }

  private void assertFromLink(String streetName, StreetMode streetMode, Vertex fromVertex) {
    var outgoing = fromVertex
      .getOutgoing()
      .iterator()
      .next()
      .getToVertex()
      .getOutgoing()
      .iterator()
      .next();

    assertEquals(
      streetName,
      outgoing.getDefaultName(),
      String.format("%s should be linked to %s", streetMode, streetName)
    );
  }

  private void assertToLink(String streetName, StreetMode streetMode, Vertex toVertex) {
    var outgoing = toVertex
      .getIncoming()
      .iterator()
      .next()
      .getFromVertex()
      .getIncoming()
      .iterator()
      .next();

    assertEquals(
      streetName,
      outgoing.getDefaultName(),
      streetMode + " should be linked to " + streetName
    );
  }

  private static final double setupStreetLongitude(int index) {
    return LONGITUDE_0 + STREET_DELTA * index;
  }

  /** Street test case */
  record StreetTC(String name, int index, double longitude, StreetTraversalPermission permissions) {
    static StreetTC of(StreetTraversalPermission permission) {
      var name =
        permission.name().charAt(0) +
        permission.name().substring(1).toLowerCase(Locale.ROOT).replace("_and_", " & ") +
        " st";
      int index = testCaseIndex++;
      double longitude = LONGITUDE_0 + STREET_DELTA * index;
      return new StreetTC(name, index, longitude, permission);
    }

    GenericLocation placeCloseToStreet() {
      return new GenericLocation("On " + name, null, LATITUDE_MIDDLE, longitude + OFFSET);
    }

    StreetEdge streetEdge(GraphRoutingTest.Builder factory, boolean wheelchair) {
      var from = factory.intersection("V" + index + "_START", LATITUDE_START, longitude);
      var to = factory.intersection("V" + index + "_END", LATITUDE_END, longitude);
      return new StreetEdgeBuilder<>()
        .withFromVertex(from)
        .withToVertex(to)
        .withGeometry(
          GeometryUtils.makeLineString(from.getLat(), from.getLon(), to.getLat(), to.getLon())
        )
        .withName(name)
        .withMeterLength(100)
        .withPermission(permissions)
        .withBack(false)
        .withWheelchairAccessible(wheelchair)
        .buildAndConnect();
    }
  }
}
