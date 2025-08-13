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
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
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
  private static final LinkingTestCase CAR_TC = LinkingTestCase.of(StreetTraversalPermission.CAR);
  private static final LinkingTestCase ALL_TC = LinkingTestCase.of(StreetTraversalPermission.ALL);
  private static final LinkingTestCase PEDESTRIAN_TC = LinkingTestCase.of(
    StreetTraversalPermission.PEDESTRIAN
  );
  private static final LinkingTestCase PEDESTRIAN_BICYCLE_TC = LinkingTestCase.of(
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
  );
  private static final LinkingTestCase BICYCLE_CAR_TC = LinkingTestCase.of(
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

  private static GenericLocation closeToCarSt() {
    return CAR_TC.placeCloseToStreet();
  }

  private static GenericLocation closeToAllSt() {
    return ALL_TC.placeCloseToStreet();
  }

  private static GenericLocation closeToPedestrianSt() {
    return PEDESTRIAN_TC.placeCloseToStreet();
  }

  private static GenericLocation closeToPedestrianAndBicycleSt() {
    return PEDESTRIAN_BICYCLE_TC.placeCloseToStreet();
  }

  private static GenericLocation closeToBicycleAndCarSt() {
    return BICYCLE_CAR_TC.placeCloseToStreet();
  }

  @BeforeEach
  protected void setUp() throws Exception {
    // Place stop in the middle of the lines(LATITUDE), and slightly above the first line

    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          CAR_TC.createStreetEdgeBuilder(this).buildAndConnect();
          ALL_TC.createStreetEdgeBuilder(this).buildAndConnect();
          PEDESTRIAN_TC.createStreetEdgeBuilder(this)
            .withWheelchairAccessible(true)
            .buildAndConnect();
          PEDESTRIAN_BICYCLE_TC.createStreetEdgeBuilder(this).buildAndConnect();
          BICYCLE_CAR_TC.createStreetEdgeBuilder(this).buildAndConnect();
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
          Arguments.of(mode, closeToCarSt(), ALL_TC),
          Arguments.of(mode, closeToAllSt(), ALL_TC),
          Arguments.of(mode, closeToPedestrianSt(), PEDESTRIAN_TC),
          Arguments.of(mode, closeToPedestrianAndBicycleSt(), PEDESTRIAN_BICYCLE_TC),
          Arguments.of(mode, closeToBicycleAndCarSt(), PEDESTRIAN_BICYCLE_TC)
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
    GenericLocation placeCloseToStreetTestCase,
    LinkingTestCase expectedStreet
  ) {
    assertLinking(placeCloseToStreetTestCase, expectedStreet, mode);
  }

  @Test
  public void testCarLinking() {
    assertLinking(closeToCarSt(), CAR_TC, CAR);
    assertLinking(closeToAllSt(), ALL_TC, CAR);
    assertLinking(closeToPedestrianSt(), ALL_TC, CAR);
    assertLinking(closeToPedestrianAndBicycleSt(), BICYCLE_CAR_TC, CAR);
    assertLinking(closeToBicycleAndCarSt(), BICYCLE_CAR_TC, CAR);
    assertLinking(stopLocation, CAR_TC, CAR_TC, CAR);
  }

  @Test
  public void testCarParkLinking() {
    assertLinking(closeToCarSt(), CAR_TC, ALL_TC, CAR_TO_PARK);
    assertLinking(closeToAllSt(), ALL_TC, ALL_TC, CAR_TO_PARK);
    assertLinking(closeToPedestrianSt(), ALL_TC, PEDESTRIAN_TC, CAR_TO_PARK);
    assertLinking(
      closeToPedestrianAndBicycleSt(),
      BICYCLE_CAR_TC,
      PEDESTRIAN_BICYCLE_TC,
      CAR_TO_PARK
    );
    assertLinking(closeToBicycleAndCarSt(), BICYCLE_CAR_TC, PEDESTRIAN_BICYCLE_TC, CAR_TO_PARK);
    assertLinking(stopLocation, CAR_TC, ALL_TC, CAR_TO_PARK);
  }

  // TODO: Linking to wheelchair accessible streets is currently not implemented,
  //       is this relevant?

  private void assertLinking(
    GenericLocation location,
    LinkingTestCase expectedStreetName,
    StreetMode... streetModes
  ) {
    assertLinking(location, expectedStreetName, expectedStreetName, streetModes);
  }

  private void assertLinking(
    GenericLocation location,
    LinkingTestCase expectedFromStreetName,
    LinkingTestCase expectedToStreetName,
    StreetMode... streetModes
  ) {
    var linker = TestVertexLinker.of(graph);
    for (final StreetMode streetMode : streetModes) {
      try (
        var temporaryVertices = new TemporaryVerticesContainer(
          graph,
          linker,
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
          linker,
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

  /**
   * A linking test case consists of a street with a given traversal permission and can be used to
   * provide a generic location close, but not directly on, the street. When linking the location
   * the test-case should be the first choice if the permissions are ok, if not link to the nearby
   * test-case streets.
   */
  record LinkingTestCase(
    String name,
    int index,
    double longitude,
    StreetTraversalPermission permissions
  ) {
    private static int indexCounter = 0;

    /**
     * Generate a street from A to B. The streets are horisontal and paralell to each other with
     * about 10-11m apart (see {@link #STREET_DELTA}).
     */
    static LinkingTestCase of(StreetTraversalPermission permission) {
      var name =
        permission.name().charAt(0) +
        permission.name().substring(1).toLowerCase(Locale.ROOT).replace("_and_", " & ") +
        " st";
      int index = indexCounter++;
      double longitude = LONGITUDE_0 + STREET_DELTA * index;
      return new LinkingTestCase(name, index, longitude, permission);
    }

    /**
     * Create a random place relativly close, but not on the street generated by this test-case.
     * The longitude for the place is the same as the street plus the {@link #OFFSET}. If this
     * test-case is street N, then the closest street for the location is in order:
     * {@code N, N+1, N-1, N+2, N-2 ... }
     */
    GenericLocation placeCloseToStreet() {
      return new GenericLocation("On " + name, null, LATITUDE_MIDDLE, longitude + OFFSET);
    }

    StreetEdgeBuilder createStreetEdgeBuilder(GraphRoutingTest.Builder factory) {
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
        .withBack(false);
    }
  }
}
