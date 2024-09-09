package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.CoordinateHelper;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class AccessEgressRouterTest extends GraphRoutingTest {

  private Graph graph;

  private TransitStopVertex stop1;
  private TransitStopVertex stop2;

  private static final CoordinateHelper origin = new CoordinateHelper(0.0, 0.0);
  private static final WgsCoordinate farAwayCoordinate = origin.east(100000);

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", origin.get());
          var B = intersection("B", origin.east(100));
          var C = intersection("C", origin.east(200));
          var D = intersection("D", origin.east(300));
          var farAway = intersection("FarAway", farAwayCoordinate);

          biStreet(A, B, 100);
          biStreet(B, C, 100);
          biStreet(C, D, 100);
          biStreet(farAway, A, 1000000);

          // Station1 has centroid routing
          var station1 = stationEntity(
            "Station1",
            b -> b.withCoordinate(A.getWgsCoordinate()).withShouldRouteToCentroid(true)
          );
          var station1Centroid = stationCentroid(station1);

          // Station2 does not have centroid routing
          var station2 = stationEntity("Station2", b -> b.withCoordinate(D.getWgsCoordinate()));
          var station2Centroid = stationCentroid(station2);

          // Stop1 is a child of station1
          stop1 = stop("Stop1", B.getWgsCoordinate(), station1);

          // Stop1 is a child of station2
          stop2 = stop("Stop2", C.getWgsCoordinate(), station2);

          biLink(A, station1Centroid);
          biLink(B, stop1);
          biLink(C, stop2);
          biLink(D, station2Centroid);
        }
      }
    );
    graph = otpModel.graph();
  }

  @Test
  void findAccessEgressFromStop() {
    var accesses = findAccessEgressFromTo(
      location("Stop1"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(Set.of("direct[Stop1]", "street[Stop1 -> Stop2]"), accesses);

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("Stop1"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(Set.of("direct[Stop1]", "street[Stop1 -> Stop2]"), egresses);
  }

  @Test
  void findAccessEgressStation() {
    // For stations with centroid routing we should use the station centroid as source for the street search
    var accesses = findAccessEgressFromTo(
      location("Station1"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(Set.of("direct[Stop1]", "street[Station1 -> Stop2]"), accesses);

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("Station1"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(Set.of("direct[Stop1]", "street[Station1 -> Stop2]"), egresses);
  }

  @Test
  void findAccessEgressStationNoCentroidRouting() {
    // For stations without centroid routing we should use the quay as source for the street search
    var accesses = findAccessEgressFromTo(
      location("Station2"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(Set.of("direct[Stop2]", "street[Stop2 -> Stop1]"), accesses);

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("Station2"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(Set.of("direct[Stop2]", "street[Stop2 -> Stop1]"), egresses);
  }

  @Test
  void findAccessEgressFromCoordinate() {
    var coordinate = origin.east(5);

    // We should get street access from coordinate to quay1 and quay2
    var accesses = findAccessEgressFromTo(
      location(coordinate),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(Set.of("street[Origin -> Stop1]", "street[Origin -> Stop2]"), accesses);

    // We should get street access from coordinate to quay1 and quay2
    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location(coordinate),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(
      Set.of("street[Destination -> Stop1]", "street[Destination -> Stop2]"),
      egresses
    );
  }

  ////////////////////
  // Helper methods //
  ////////////////////

  private GenericLocation location(WgsCoordinate coordinate) {
    return new GenericLocation(coordinate.latitude(), coordinate.longitude());
  }

  private GenericLocation location(FeedScopedId id) {
    return new GenericLocation(null, id, null, null);
  }

  private GenericLocation location(String id) {
    return location(new FeedScopedId("F", id));
  }

  private RouteRequest requestFromTo(GenericLocation from, GenericLocation to) {
    var routeRequest = new RouteRequest();
    routeRequest.setFrom(from);
    routeRequest.setTo(to);
    return routeRequest;
  }

  private String nearbyStopDescription(NearbyStop nearbyStop) {
    if (nearbyStop.edges.isEmpty()) {
      return "direct[" + nearbyStop.stop.getName() + "]";
    } else {
      return "street[" + stateDescription(nearbyStop.state) + "]";
    }
  }

  private String stateDescription(State state) {
    var last = state;
    while (last.getBackState() != null) {
      last = last.getBackState();
    }
    return last.getVertex().getName() + " -> " + state.getVertex().getName();
  }

  private void assertAcessEgresses(Set<String> expected, Collection<NearbyStop> actual) {
    assertThat(actual.stream().map(this::nearbyStopDescription))
      .containsExactlyElementsIn(expected);
  }

  private Collection<NearbyStop> findAccessEgressFromTo(
    GenericLocation from,
    GenericLocation to,
    AccessEgressType accessEgress
  ) {
    var maxStopCount = 10;
    var durationLimit = Duration.ofMinutes(10);
    var request = requestFromTo(from, to);

    try (
      var verticesContainer = new TemporaryVerticesContainer(
        graph,
        from,
        to,
        StreetMode.WALK,
        StreetMode.WALK
      )
    ) {
      return AccessEgressRouter.findAccessEgresses(
        request,
        verticesContainer,
        new StreetRequest(),
        null,
        accessEgress,
        durationLimit,
        maxStopCount
      );
    }
  }
}
