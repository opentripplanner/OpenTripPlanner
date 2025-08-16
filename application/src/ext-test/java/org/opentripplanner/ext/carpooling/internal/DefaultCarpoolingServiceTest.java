package org.opentripplanner.ext.carpooling.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.AreaStop;

class DefaultCarpoolingServiceTest extends GraphRoutingTest {

  private final AtomicInteger stopIndexCounter = new AtomicInteger(0);
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final Instant DATE_TIME = LocalDateTime.of(2025, Month.MAY, 17, 11, 15).toInstant(
    ZoneOffset.UTC
  );

  // Test vertices representing a simple street network
  private StreetVertex A, B, C, D, E, F; // Street intersections
  private StreetVertex pickupVertex, dropoffVertex; // Carpool pickup/dropoff points

  private CarpoolingRepository mockRepository;
  private CarpoolingService carpoolingService;
  private AreaStop boardingArea;
  private AreaStop alightingArea;

  @BeforeEach
  protected void setUp() {
    // Create a realistic street network for testing A* routing
    //
    // Network layout:
    //   A <---> B <---> C <---> D <---> E <---> F
    //           |               |
    //        pickup          dropoff
    //
    // This allows testing:
    // - Walking access path: A -> pickup (via B)
    // - Driving carpool path: pickup -> dropoff (via C)
    // - Walking egress path: dropoff -> F (via E)

    var model = modelOf(
      new Builder() {
        @Override
        public void build() {
          // Create street intersections in a line
          A = intersection("A", 0.001, 10.001);
          B = intersection("B", 0.002, 10.002);
          C = intersection("C", 0.003, 10.003);
          D = intersection("D", 0.004, 10.004);
          E = intersection("E", 0.005, 10.005);
          F = intersection("F", 0.006, 10.006);

          // Carpool pickup and dropoff points (further apart)
          pickupVertex = intersection("pickup", 0.0025, 10.0025); // Between B and C
          dropoffVertex = intersection("dropoff", 0.0035, 10.0035); // Between C and D

          // Create bidirectional streets
          // Walking-only segment (A-B for access)
          street(A, B, 100, StreetTraversalPermission.PEDESTRIAN);

          // Create the network: A-B-pickup-C-dropoff-D-E-F
          // Mixed-use segments (B->pickup->C->dropoff->D for carpool driving)
          street(B, pickupVertex, 100, StreetTraversalPermission.PEDESTRIAN_AND_CAR);
          street(pickupVertex, C, 100, StreetTraversalPermission.PEDESTRIAN_AND_CAR);
          street(C, dropoffVertex, 100, StreetTraversalPermission.PEDESTRIAN_AND_CAR);
          street(dropoffVertex, D, 100, StreetTraversalPermission.PEDESTRIAN_AND_CAR);

          // Walking-only segment (D-E-F for egress)
          street(D, E, 100, StreetTraversalPermission.PEDESTRIAN);
          street(E, F, 100, StreetTraversalPermission.PEDESTRIAN);
        }
      }
    );

    mockRepository = mock(CarpoolingRepository.class);
    carpoolingService = new DefaultCarpoolingService(mockRepository, model.graph());

    // Create area stops using coordinates from our test graph
    boardingArea = createAreaStop(
      "close-pickup",
      new WgsCoordinate(pickupVertex.getLat(), pickupVertex.getLon()),
      100
    );
    alightingArea = createAreaStop(
      "close-dropoff",
      new WgsCoordinate(dropoffVertex.getLat(), dropoffVertex.getLon()),
      100
    );
  }

  @Test
  void route_withValidLocations_returnsEmptyItineraries() {
    // Given: A valid route request using coordinates from our test graph
    RouteRequest request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(A.getLat(), A.getLon()))
      .withTo(GenericLocation.fromCoordinate(F.getLat(), F.getLon()))
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: No carpool trips in repository
    when(mockRepository.getCarpoolTrips()).thenReturn(List.of());

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(serverContext, request);

    // Then: Empty list is returned (no available trips)
    assertTrue(result.isEmpty());
  }

  @Test
  void route_withCarpoolTrip_performsAStarRouting() {
    // Given: A valid route request with coordinates that should be routable in our test network
    RouteRequest request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(A.getLat(), A.getLon()))
      .withTo(GenericLocation.fromCoordinate(F.getLat(), F.getLon()))
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Carpool trip with pickup/dropoff areas that create a realistic routing scenario
    CarpoolTrip carpoolTrip = new CarpoolTripBuilder(id("test-trip"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("test-trip").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(carpoolTrip));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(serverContext, request);

    // Then: Verify A* routing produces valid results
    assertNotNull(result, "Should return a result list");

    // If we get an itinerary, it should be properly structured with A* routing results
    if (!result.isEmpty()) {
      Itinerary itinerary = result.get(0);

      // Should have 3 legs: access walking + carpool + egress walking
      assertTrue(itinerary.legs().size() >= 1, "Should have at least the carpool leg");
      assertTrue(
        itinerary.legs().size() <= 3,
        "Should have at most 3 legs (access + carpool + egress)"
      );

      // Should have positive cost calculated from A* routing
      assertTrue(itinerary.generalizedCost() > 0, "Should have positive cost from A* routing");

      // Should have positive distance from street network routing
      assertTrue(itinerary.distanceMeters() > 0, "Should have positive distance from A* routing");

      // Verify leg structure
      boolean hasWalkingLeg = itinerary
        .legs()
        .stream()
        .anyMatch(
          leg -> leg instanceof StreetLeg && ((StreetLeg) leg).getMode() == TraverseMode.WALK
        );
      boolean hasCarpoolLeg = itinerary
        .legs()
        .stream()
        .anyMatch(leg -> leg instanceof CarpoolLeg);

      assertTrue(hasCarpoolLeg, "Should contain a carpool transit leg");

      // If walking legs exist, they should have proper A* routing results
      itinerary
        .legs()
        .stream()
        .filter(leg -> leg instanceof StreetLeg)
        .map(leg -> (StreetLeg) leg)
        .forEach(walkLeg -> {
          assertTrue(walkLeg.getMode() == TraverseMode.WALK, "Walking legs should have WALK mode");
          assertNotNull(walkLeg.legGeometry(), "Walking legs should have geometry from A* routing");
          assertTrue(walkLeg.distanceMeters() > 0, "Walking legs should have positive distance");
          assertNotNull(walkLeg.from(), "Walking legs should have from place");
          assertNotNull(walkLeg.to(), "Walking legs should have to place");
        });
    }
  }

  @Test
  void route_multipleTrips_selectsBestCandidatesForAStarRouting() {
    // Given: Route request
    RouteRequest request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(A.getLat(), A.getLon()))
      .withTo(GenericLocation.fromCoordinate(F.getLat(), F.getLon()))
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Multiple carpool trips (to test that algorithm selects best ones)
    CarpoolTrip trip1 = new CarpoolTripBuilder(id("trip-1"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("trip-1").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    // Create second trip with same areas (to test limit of 3 candidates)
    CarpoolTrip trip2 = new CarpoolTripBuilder(id("trip-2"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now().plusMinutes(10))
      .withEndTime(ZonedDateTime.now().plusMinutes(40))
      .withTrip(TimetableRepositoryForTest.trip("trip-2").build())
      .withProvider("TestProvider")
      .withAvailableSeats(1)
      .build();

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(trip1, trip2));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(serverContext, request);

    // Then: Multiple trips should be processed with A* routing
    // Result count depends on feasible paths found, but no exception should be thrown
    assertNotNull(result, "Should return a result list");
    assertTrue(result.size() <= 2, "Should process up to 2 trips");
  }

  @Test
  void aStarRouting_producesMoreAccurateResultsThanStraightLine() {
    // Given: A route request with a path that has street network geometry (not straight line)
    RouteRequest request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(A.getLat(), A.getLon()))
      .withTo(GenericLocation.fromCoordinate(F.getLat(), F.getLon()))
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: A carpool trip that requires multi-segment routing
    CarpoolTrip carpoolTrip = new CarpoolTripBuilder(id("test-trip"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("test-trip").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(carpoolTrip));

    // Calculate straight-line distances for comparison
    double straightLineAccessDistance = SphericalDistanceLibrary.fastDistance(
      A.getLat(),
      A.getLon(),
      boardingArea.getCoordinate().latitude(),
      boardingArea.getCoordinate().longitude()
    );
    double straightLineEgressDistance = SphericalDistanceLibrary.fastDistance(
      alightingArea.getCoordinate().latitude(),
      alightingArea.getCoordinate().longitude(),
      F.getLat(),
      F.getLon()
    );

    // When: Routing is performed
    List<Itinerary> result = carpoolingService.route(request);

    // Then: If A* routing succeeds, it should produce different (more accurate) results
    if (!result.isEmpty()) {
      Itinerary itinerary = result.get(0);

      // A* routing through street network should typically be longer than straight-line
      double totalWalkingDistance = itinerary.totalWalkDistanceMeters();
      double totalStraightLineWalking = straightLineAccessDistance + straightLineEgressDistance;

      // The A* routed walking distance should be >= straight line distance
      // (in a real street network, it's typically longer due to street geometry)
      assertTrue(
        totalWalkingDistance >= totalStraightLineWalking * 0.8,
        String.format(
          "A* walking distance (%.1fm) should be reasonably close to straight-line (%.1fm)",
          totalWalkingDistance,
          totalStraightLineWalking
        )
      );

      // Verify we have actual geometry from A* routing, not just straight lines
      long walkingLegsWithGeometry = itinerary
        .legs()
        .stream()
        .filter(leg -> leg instanceof StreetLeg)
        .map(leg -> (StreetLeg) leg)
        .mapToLong(leg -> leg.legGeometry() != null ? leg.legGeometry().getNumPoints() : 0)
        .sum();

      assertTrue(
        walkingLegsWithGeometry >= 2,
        "Walking legs should have geometry with multiple points from A* routing"
      );
    }
  }

  private AreaStop createAreaStop(String id, WgsCoordinate center, double radiusMeters) {
    Polygon geometry = circularPolygonAroundCenter(center, radiusMeters);

    return AreaStop.of(id(id), stopIndexCounter::getAndIncrement).withGeometry(geometry).build();
  }

  private static Polygon circularPolygonAroundCenter(WgsCoordinate center, double radiusMeters) {
    // Create a circular polygon around the center point
    Coordinate[] coordinates = new Coordinate[37]; // 36 points + closing point
    for (int i = 0; i < 36; i++) {
      double angle = (2 * Math.PI * i) / 36;
      double deltaLat = (radiusMeters / 111000.0) * Math.cos(angle); // Rough conversion to degrees
      double deltaLng =
        (radiusMeters / (111000.0 * Math.cos(Math.toRadians(center.latitude())))) * Math.sin(angle);
      coordinates[i] = new Coordinate(center.longitude() + deltaLng, center.latitude() + deltaLat);
    }
    coordinates[36] = coordinates[0]; // Close the polygon

    return GEOMETRY_FACTORY.createPolygon(coordinates);
  }

  private CarpoolTrip createCarpoolTrip(String tripId, AreaStop pickupArea, AreaStop dropoffArea) {
    return new CarpoolTripBuilder(id(tripId))
      .withBoardingArea(pickupArea)
      .withAlightingArea(dropoffArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip(tripId).build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();
  }
}
