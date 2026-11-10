package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.ext.taxi.TaxiService;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.mapping.LinkingContextRequestMapper;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;

/**
 * The street network is a line: ORIGIN --2000m-- VIA --100m-- STOP --far-- FAR_AWAY. The distance
 * between the origin and the via location is long enough for driving to be clearly faster than
 * walking.
 */
class ViaAccessEgressRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(60.0, 25.0);
  private static final WgsCoordinate VIA = ORIGIN.moveEastMeters(2000);
  private static final WgsCoordinate STOP = ORIGIN.moveEastMeters(2100);
  private static final WgsCoordinate FAR_AWAY = ORIGIN.moveEastMeters(100000);
  private static final VisitViaLocation VIA_LOCATION = new VisitViaLocation(
    "via",
    Duration.ZERO,
    List.of(),
    VIA
  );

  private Graph graph;
  private TransitRepository transitRepository;
  private TimetableDataImport timetableDataImport;

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var origin = intersection("A", ORIGIN);
          var via = intersection("B", VIA);
          var c = intersection("C", STOP);
          var farAway = intersection("FarAway", FAR_AWAY);

          biStreet(origin, via, 2000);
          biStreet(via, c, 100);
          biStreet(c, farAway, 1000000);

          var stop = stop("S", b -> b.withCoordinate(STOP));
          biLink(c, stop);
        }
      }
    );
    graph = otpModel.graph();
    transitRepository = otpModel.transitRepository();
    timetableDataImport = otpModel.timetableDataImport();
  }

  @ParameterizedTest
  @EnumSource(names = { "CAR_PICKUP", "CAR_HAILING", "TAXI" })
  void pickupAccessIsOnlyUsedToTheFirstViaLocation(StreetMode mode) {
    var request = request(location(ORIGIN), location(FAR_AWAY), mode, StreetMode.WALK);

    var accesses = find(request, mode, AccessEgressType.ACCESS, null);

    var viaAccess = findViaAccessEgress(accesses);
    var originToVia = viaAccess.finalStates.get(0);
    var viaToStop = viaAccess.finalStates.get(1);
    assertThat(modes(originToVia)).contains(TraverseMode.CAR);
    assertThat(modes(viaToStop)).doesNotContain(TraverseMode.CAR);
  }

  @ParameterizedTest
  @EnumSource(names = { "CAR_PICKUP", "CAR_HAILING", "TAXI" })
  void pickupEgressIsOnlyUsedFromTheLastViaLocation(StreetMode mode) {
    var request = request(location(FAR_AWAY), location(ORIGIN), StreetMode.WALK, mode);

    var egresses = find(request, mode, AccessEgressType.EGRESS, null);

    var viaEgress = findViaAccessEgress(egresses);
    var stopToVia = viaEgress.finalStates.get(0);
    var viaToDestination = viaEgress.finalStates.get(1);
    assertThat(modes(stopToVia)).doesNotContain(TraverseMode.CAR);
    assertThat(modes(viaToDestination)).contains(TraverseMode.CAR);
  }

  @Test
  void egressUsesTheEgressModeAndNotTheAccessMode() {
    var request = request(
      location(FAR_AWAY),
      location(ORIGIN),
      StreetMode.NOT_SET,
      StreetMode.TAXI
    );

    var egresses = find(request, StreetMode.TAXI, AccessEgressType.EGRESS, null);

    var viaEgress = findViaAccessEgress(egresses);
    assertThat(modes(viaEgress.finalStates.get(1))).contains(TraverseMode.CAR);
  }

  @Test
  void viaAccessIsFoundWhenTaxiDoesNotCoverStopsNearTheOrigin() {
    var request = request(location(ORIGIN), location(FAR_AWAY), StreetMode.TAXI, StreetMode.WALK);

    var accesses = find(
      request,
      StreetMode.TAXI,
      AccessEgressType.ACCESS,
      new CoverageTaxiService(true)
    );

    var viaAccess = findViaAccessEgress(accesses);
    assertThat(modes(viaAccess.finalStates.get(1))).doesNotContain(TraverseMode.CAR);
    assertThat(
      accesses
        .stream()
        .filter(it -> it.finalStates.size() == 1)
        .toList()
    ).isEmpty();
  }

  @Test
  void viaAccessIsDroppedWhenTaxiDoesNotCoverTheOriginAndTheViaLocation() {
    var request = request(location(ORIGIN), location(FAR_AWAY), StreetMode.TAXI, StreetMode.WALK);

    var accesses = find(
      request,
      StreetMode.TAXI,
      AccessEgressType.ACCESS,
      new CoverageTaxiService(false)
    );

    assertThat(accesses).isEmpty();
  }

  @Test
  void viaEgressIsDroppedWhenTaxiDoesNotCoverTheViaLocationAndTheDestination() {
    var request = request(location(FAR_AWAY), location(ORIGIN), StreetMode.WALK, StreetMode.TAXI);

    var egresses = find(
      request,
      StreetMode.TAXI,
      AccessEgressType.EGRESS,
      new CoverageTaxiService(false)
    );

    assertThat(egresses).isEmpty();
  }

  /* Helper methods */

  private static NearbyStop findViaAccessEgress(Collection<NearbyStop> accessEgresses) {
    var viaAccessEgresses = accessEgresses
      .stream()
      .filter(it -> it.finalStates.size() == 2)
      .toList();
    assertThat(viaAccessEgresses).hasSize(1);
    return viaAccessEgresses.getFirst();
  }

  private static List<TraverseMode> modes(State state) {
    var modes = new ArrayList<TraverseMode>();
    for (var s = state; s != null; s = s.getBackState()) {
      if (s.getBackMode() != null) {
        modes.add(s.getBackMode());
      }
    }
    return modes;
  }

  private static GenericLocation location(WgsCoordinate coordinate) {
    return GenericLocation.fromCoordinate(coordinate.latitude(), coordinate.longitude());
  }

  private static RouteRequest request(
    GenericLocation from,
    GenericLocation to,
    StreetMode accessMode,
    StreetMode egressMode
  ) {
    return RouteRequest.of()
      .withDateTime(Instant.parse("2026-05-13T12:00:00Z"))
      .withFrom(from)
      .withTo(to)
      .withViaLocations(List.of(VIA_LOCATION))
      .withJourney(jb ->
        jb.withAccess(new StreetRequest(accessMode)).withEgress(new StreetRequest(egressMode))
      )
      .buildRequest();
  }

  private Collection<NearbyStop> find(
    RouteRequest request,
    StreetMode streetMode,
    AccessEgressType accessEgress,
    TaxiService taxiService
  ) {
    try (var verticesContainer = new TemporaryVerticesContainer()) {
      var vertexLinker = VertexLinkerTestFactory.of(graph);
      var vertexCreationService = new VertexCreationService(vertexLinker);
      var transitService = new DefaultTransitService(
        transitRepository,
        DefaultTimetableRepository.scheduledOnly(timetableDataImport.toScheduledTimetableData())
      );
      var linkingContextFactory = new LinkingContextFactory(
        graph,
        vertexCreationService,
        transitService::findStopOrChildIds,
        id ->
          Optional.ofNullable(transitService.getStopLocationsGroup(id)).map(group ->
            group.getCoordinate()
          )
      );
      var linkingContext = linkingContextFactory.create(
        verticesContainer,
        LinkingContextRequestMapper.map(request)
      );

      return new ViaAccessEgressRouter().findAccessEgresses(
        request,
        streetMode,
        List.of(),
        accessEgress,
        Duration.ofMinutes(45),
        10,
        linkingContext,
        StreetLimitationParametersService.DEFAULT,
        GeofencingZoneService.EMPTY,
        transitService,
        taxiService
      );
    }
  }

  /**
   * Taxi service which doesn't cover any stops, and either covers all or no pickup/drop-off
   * coordinate pairs.
   */
  private record CoverageTaxiService(boolean covered) implements TaxiService {
    @Override
    public Collection<NearbyStop> filterNearbyStops(
      TransitService transitService,
      Collection<NearbyStop> nearbyStops,
      AccessEgressType type,
      RouteRequest request
    ) {
      return List.of();
    }

    @Override
    public boolean isCoveredByCommonProvider(WgsCoordinate pickup, WgsCoordinate dropoff) {
      return covered;
    }

    @Override
    public List<Leg> decorateAccessEgressLegs(
      List<Leg> legs,
      WgsCoordinate pickup,
      WgsCoordinate dropoff
    ) {
      return legs;
    }

    @Override
    public List<Itinerary> routeDirect(
      TransitService transitService,
      RouteRequest request,
      LinkingContext linkingContext
    ) {
      return List.of();
    }
  }
}
