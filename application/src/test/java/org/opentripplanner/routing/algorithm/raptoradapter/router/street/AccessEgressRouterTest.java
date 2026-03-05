package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.module.nearbystops.SiteRepositoryResolver;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.TemporaryVerticesContainer;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.mapping.LinkingContextRequestMapper;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

class AccessEgressRouterTest extends GraphRoutingTest {

  private Graph graph;
  private TimetableRepository timetableRepository;

  private TransitStopVertex stopForCentroidRoutingStation;
  private TransitStopVertex stopForNoCentroidRoutingStation;

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(0.0, 0.0);
  private static final WgsCoordinate FAR_AWAY_COORDINATE = ORIGIN.moveEastMeters(100000);

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(100));
          var C = intersection("C", ORIGIN.moveEastMeters(200));
          var D = intersection("D", ORIGIN.moveEastMeters(300));
          var farAway = intersection("FarAway", FAR_AWAY_COORDINATE);

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

          // StopForCentroidRoutingStation is a child of centroidRoutingStation
          stopForCentroidRoutingStation = stop("StopForCentroidRoutingStation", b ->
            b.withCoordinate(B.toWgsCoordinate()).withParentStation(centroidRoutingStation)
          );

          // StopForNoCentroidRoutingStation is a child of noCentroidRoutingStation
          stopForNoCentroidRoutingStation = stop("StopForNoCentroidRoutingStation", b ->
            b.withCoordinate(C.toWgsCoordinate()).withParentStation(noCentroidRoutingStation)
          );

          biLink(A, centroidRoutingStationVertex);
          biLink(B, stopForCentroidRoutingStation);
          biLink(C, stopForNoCentroidRoutingStation);
        }
      }
    );
    graph = otpModel.graph();
    timetableRepository = otpModel.timetableRepository();
  }

  @Test
  void findAccessEgressFromStop() {
    var accesses = findAccessEgressFromTo(
      location("StopForCentroidRoutingStation"),
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
    var coordinate = ORIGIN.moveEastMeters(5);

    // We should get street access from coordinate to quay1 and quay2
    var accesses = findAccessEgressFromTo(
      location(coordinate),
      location(FAR_AWAY_COORDINATE),
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
      location(FAR_AWAY_COORDINATE),
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
    return GenericLocation.fromCoordinate(coordinate.latitude(), coordinate.longitude());
  }

  private GenericLocation location(FeedScopedId id) {
    return new GenericLocation(null, id, null, null);
  }

  private GenericLocation location(String id) {
    return location(new FeedScopedId("F", id));
  }

  private RouteRequest requestFromTo(GenericLocation from, GenericLocation to) {
    return RouteRequest.of().withFrom(from).withTo(to).buildRequest();
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

    try (var verticesContainer = new TemporaryVerticesContainer()) {
      var vertexLinker = VertexLinkerTestFactory.of(graph);
      var vertexCreationService = new VertexCreationService(vertexLinker);
      var transitService = new DefaultTransitService(timetableRepository);
      var linkingContextFactory = new LinkingContextFactory(
        graph,
        vertexCreationService,
        transitService::findStopOrChildIds,
        id -> {
          var group = transitService.getStopLocationsGroup(id);
          return Optional.ofNullable(group).map(locationsGroup -> locationsGroup.getCoordinate());
        }
      );
      var linkingRequest = LinkingContextRequestMapper.map(request);
      var linkingContext = linkingContextFactory.create(verticesContainer, linkingRequest);

      return new AccessEgressRouter(
        new SiteRepositoryResolver(timetableRepository.getSiteRepository())
      ).findAccessEgresses(
        request,
        StreetMode.WALK,
        List.of(),
        accessEgress,
        durationLimit,
        maxStopCount,
        linkingContext
      );
    }
  }
}
