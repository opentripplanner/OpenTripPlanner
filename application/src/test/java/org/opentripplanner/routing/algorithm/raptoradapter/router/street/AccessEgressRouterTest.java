package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  private TransitStopVertex stopForCentroidRoutingStation;
  private TransitStopVertex stopForNoCentroidRoutingStation;

  private static final WgsCoordinate origin = new WgsCoordinate(0.0, 0.0);
  private static final WgsCoordinate farAwayCoordinate = origin.moveEastMeters(100000);

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", origin);
          var B = intersection("B", origin.moveEastMeters(100));
          var C = intersection("C", origin.moveEastMeters(200));
          var D = intersection("D", origin.moveEastMeters(300));
          var farAway = intersection("FarAway", farAwayCoordinate);

          biStreet(A, B, 100);
          biStreet(B, C, 100);
          biStreet(C, D, 100);
          biStreet(farAway, A, 1000000);

          var centroidRoutingStation = stationEntity("CentroidRoutingStation", b ->
            b.withCoordinate(A.toWgsCoordinate()).withShouldRouteToCentroid(true)
          );
          var centroidRoutingStationVertex = stationCentroid(centroidRoutingStation);

          var noCentroidRoutingStation = stationEntity("NoCentroidRoutingStation", b ->
            b.withCoordinate(D.toWgsCoordinate())
          );
          var noCentroidRoutingStationVertex = stationCentroid(noCentroidRoutingStation);

          // StopForCentroidRoutingStation is a child of centroidRoutingStation
          stopForCentroidRoutingStation = stop(
            "StopForCentroidRoutingStation",
            B.toWgsCoordinate(),
            centroidRoutingStation
          );

          // StopForNoCentroidRoutingStation is a child of noCentroidRoutingStation
          stopForNoCentroidRoutingStation = stop(
            "StopForNoCentroidRoutingStation",
            C.toWgsCoordinate(),
            noCentroidRoutingStation
          );

          biLink(A, centroidRoutingStationVertex);
          biLink(B, stopForCentroidRoutingStation);
          biLink(C, stopForNoCentroidRoutingStation);
          biLink(D, noCentroidRoutingStationVertex);
        }
      }
    );
    graph = otpModel.graph();
  }

  @Test
  void findAccessEgressFromStop() {
    var accesses = findAccessEgressFromTo(
      location("StopForCentroidRoutingStation"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForCentroidRoutingStation]",
        "street[StopForCentroidRoutingStation -> StopForNoCentroidRoutingStation]"
      ),
      accesses
    );

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("StopForCentroidRoutingStation"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForCentroidRoutingStation]",
        "street[StopForCentroidRoutingStation -> StopForNoCentroidRoutingStation]"
      ),
      egresses
    );
  }

  @Test
  void findAccessEgressStation() {
    // For stations with centroid routing we should use the station centroid as source for the street search
    var accesses = findAccessEgressFromTo(
      location("CentroidRoutingStation"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForCentroidRoutingStation]",
        "street[CentroidRoutingStation -> StopForNoCentroidRoutingStation]"
      ),
      accesses
    );

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("CentroidRoutingStation"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForCentroidRoutingStation]",
        "street[CentroidRoutingStation -> StopForNoCentroidRoutingStation]"
      ),
      egresses
    );
  }

  @Test
  void findAccessEgressStationNoCentroidRouting() {
    // For stations without centroid routing we should use the quay as source for the street search
    var accesses = findAccessEgressFromTo(
      location("NoCentroidRoutingStation"),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForNoCentroidRoutingStation]",
        "street[StopForNoCentroidRoutingStation -> StopForCentroidRoutingStation]"
      ),
      accesses
    );

    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location("NoCentroidRoutingStation"),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(
      Set.of(
        "direct[StopForNoCentroidRoutingStation]",
        "street[StopForNoCentroidRoutingStation -> StopForCentroidRoutingStation]"
      ),
      egresses
    );
  }

  @Test
  void findAccessEgressFromCoordinate() {
    var coordinate = origin.moveEastMeters(5);

    // We should get street access from coordinate to quay1 and quay2
    var accesses = findAccessEgressFromTo(
      location(coordinate),
      location(farAwayCoordinate),
      AccessEgressType.ACCESS
    );
    assertAcessEgresses(
      Set.of(
        "street[Origin -> StopForCentroidRoutingStation]",
        "street[Origin -> StopForNoCentroidRoutingStation]"
      ),
      accesses
    );

    // We should get street access from coordinate to quay1 and quay2
    var egresses = findAccessEgressFromTo(
      location(farAwayCoordinate),
      location(coordinate),
      AccessEgressType.EGRESS
    );
    assertAcessEgresses(
      Set.of(
        "street[Destination -> StopForCentroidRoutingStation]",
        "street[Destination -> StopForNoCentroidRoutingStation]"
      ),
      egresses
    );
  }

  /* Helper methods */

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
    assertThat(actual.stream().map(this::nearbyStopDescription)).containsExactlyElementsIn(
      expected
    );
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
