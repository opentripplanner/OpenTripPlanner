package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

/**
 * This tests linking of GenericLocations to streets for each StreetMode. The test has 5 parallel
 * streets and a linking is performed for each mode in the middle of each street. Currently linking
 * is handled in three different ways: for CAR, CAR_PARK and everything else, which is reflected in
 * the tests.
 */
public class StreetModeLinkingTest extends GraphRoutingTest {

  private Graph graph;

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          street(
            intersection("A1", 47.5000, 19.00),
            intersection("A2", 47.5020, 19.00),
            100,
            StreetTraversalPermission.CAR
          );

          street(
            intersection("B1", 47.5000, 19.01),
            intersection("B2", 47.5020, 19.01),
            100,
            StreetTraversalPermission.ALL
          );

          street(
            intersection("C1", 47.5000, 19.02),
            intersection("C2", 47.5020, 19.02),
            100,
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
          );

          streetBuilder(
            intersection("D1", 47.500, 19.03),
            intersection("D2", 47.502, 19.03),
            100,
            StreetTraversalPermission.PEDESTRIAN
          )
            .withWheelchairAccessible(false)
            .buildAndConnect();

          street(
            intersection("E1", 47.500, 19.04),
            intersection("E2", 47.502, 19.04),
            100,
            StreetTraversalPermission.BICYCLE_AND_CAR
          );

          stop("STOP", 47.501, 19.04);
        }
      }
    );
    graph = otpModel.graph();

    graph.hasStreets = true;
    TestStreetLinkerModule.link(graph, otpModel.timetableRepository());
  }

  @Test
  public void testCarLinking() {
    /*
    assertLinkedFromTo(47.501, 19.00, "A1A2 street", StreetMode.CAR);
    assertLinkedFromTo(47.501, 19.01, "B1B2 street", StreetMode.CAR);
    assertLinkedFromTo(47.501, 19.02, "B1B2 street", StreetMode.CAR);
    assertLinkedFromTo(47.501, 19.03, "E1E2 street", StreetMode.CAR);
    assertLinkedFromTo(47.501, 19.04, "E1E2 street", StreetMode.CAR);
     */
    assertLinkedFromTo("STOP", "E1E2 street", StreetMode.CAR);
  }

  @Test
  public void testCarParkLinking() {
    var setup = (BiFunction<Double, Double, Consumer<RouteRequest>>) (
      Double latitude,
      Double longitude
    ) ->
      (RouteRequest rr) -> {
        rr.setFrom(new GenericLocation(latitude, longitude));
        rr.setTo(new GenericLocation(latitude, longitude));
      };

    assertLinking(setup.apply(47.501, 19.00), "A1A2 street", "B1B2 street", StreetMode.CAR_TO_PARK);
    assertLinking(setup.apply(47.501, 19.01), "B1B2 street", "B1B2 street", StreetMode.CAR_TO_PARK);
    assertLinking(setup.apply(47.501, 19.02), "B1B2 street", "C1C2 street", StreetMode.CAR_TO_PARK);
    assertLinking(setup.apply(47.501, 19.03), "E1E2 street", "D1D2 street", StreetMode.CAR_TO_PARK);
    assertLinking(setup.apply(47.501, 19.04), "E1E2 street", "D1D2 street", StreetMode.CAR_TO_PARK);
    assertLinking(
      rr -> {
        rr.setFrom(new GenericLocation(null, TimetableRepositoryForTest.id("STOP"), null, null));
        rr.setTo(new GenericLocation(null, TimetableRepositoryForTest.id("STOP"), null, null));
      },
      "E1E2 street",
      "D1D2 street",
      StreetMode.CAR_TO_PARK
    );
  }

  // Only CAR linking is handled specially, since walking with a bike is always a possibility,
  // and so no difference is made between BIKE/WALK:

  @Test
  public void testDefaultLinking() {
    var streetModes = new StreetMode[] {
      StreetMode.WALK,
      StreetMode.BIKE,
      StreetMode.BIKE_TO_PARK,
      StreetMode.BIKE_RENTAL,
      StreetMode.SCOOTER_RENTAL,
      StreetMode.FLEXIBLE,
      StreetMode.CAR_PICKUP,
      StreetMode.CAR_RENTAL,
    };

    assertLinkedFromTo(47.501, 19.00, "B1B2 street", streetModes);
    assertLinkedFromTo(47.501, 19.01, "B1B2 street", streetModes);
    assertLinkedFromTo(47.501, 19.02, "C1C2 street", streetModes);
    assertLinkedFromTo(47.501, 19.03, "D1D2 street", streetModes);
    assertLinkedFromTo(47.501, 19.04, "D1D2 street", streetModes);
    assertLinkedFromTo("STOP", "D1D2 street", streetModes);
  }

  // Linking to wheelchair accessible streets is currently not implemented.

  @Test
  @Disabled
  public void testWheelchairLinking() {
    assertLinking(
      rr -> {
        rr.setFrom(new GenericLocation(47.5010, 19.03));
        rr.setTo(new GenericLocation(47.5010, 19.03));
        rr.setWheelchair(true);
      },
      "C1C2 street",
      "C1C2 street",
      StreetMode.WALK
    );
  }

  private void assertLinkedFromTo(
    double latitude,
    double longitude,
    String streetName,
    StreetMode... streetModes
  ) {
    assertLinking(
      rr -> {
        rr.setFrom(new GenericLocation(latitude, longitude));
        rr.setTo(new GenericLocation(latitude, longitude));
      },
      streetName,
      streetName,
      streetModes
    );
  }

  private void assertLinkedFromTo(String stopId, String streetName, StreetMode... streetModes) {
    assertLinking(
      rr -> {
        rr.setFrom(new GenericLocation(null, TimetableRepositoryForTest.id(stopId), null, null));
        rr.setTo(new GenericLocation(null, TimetableRepositoryForTest.id(stopId), null, null));
      },
      streetName,
      streetName,
      streetModes
    );
  }

  private void assertLinking(
    Consumer<RouteRequest> consumer,
    String fromStreetName,
    String toStreetName,
    StreetMode... streetModes
  ) {
    for (final StreetMode streetMode : streetModes) {
      var routingRequest = new RouteRequest();

      consumer.accept(routingRequest);

      // Remove to, so that origin and destination are different
      routingRequest.setTo(new GenericLocation(null, null));

      try (
        var temporaryVertices = new TemporaryVerticesContainer(
          graph,
          routingRequest.from(),
          routingRequest.to(),
          streetMode,
          streetMode
        )
      ) {
        if (fromStreetName != null) {
          assertFromLink(
            fromStreetName,
            streetMode,
            temporaryVertices.getFromVertices().iterator().next()
          );
        }
      }

      routingRequest = new RouteRequest();

      consumer.accept(routingRequest);

      // Remove from, so that origin and destination are different
      routingRequest.setFrom(new GenericLocation(null, null));

      try (
        var temporaryVertices = new TemporaryVerticesContainer(
          graph,
          routingRequest.from(),
          routingRequest.to(),
          streetMode,
          streetMode
        )
      ) {
        if (toStreetName != null) {
          assertToLink(
            toStreetName,
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
}
