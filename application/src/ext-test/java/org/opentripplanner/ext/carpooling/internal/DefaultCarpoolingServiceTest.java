package org.opentripplanner.ext.carpooling.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.AreaStop;

class DefaultCarpoolingServiceTest {

  private final AtomicInteger stopIndexCounter = new AtomicInteger(0);
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  // Test coordinates (Oslo area)

  // Downtown Oslo area
  private static final GenericLocation FROM_LOCATION = GenericLocation.fromCoordinate(
    59.9139,
    10.7522
  );

  // Nydalen area in Oslo
  private static final GenericLocation TO_LOCATION = GenericLocation.fromCoordinate(
    59.9496,
    10.7568
  );

  // Test coordinates for area stops
  // Pilestredet Park in Oslo
  private static final WgsCoordinate PICKUP_CENTER = new WgsCoordinate(59.9200, 10.7400); // Close to FROM

  // Voldsløkka in Oslo
  private static final WgsCoordinate DROPOFF_CENTER = new WgsCoordinate(59.9450, 10.7600); // Close to TO

  // Outside of Kjeller, Lillestrøm area
  private static final WgsCoordinate FAR_PICKUP_CENTER = new WgsCoordinate(60.0000, 11.0000); // Far from FROM

  private static final Instant DATE_TIME = LocalDateTime.of(2025, Month.MAY, 17, 11, 15).toInstant(
    ZoneOffset.UTC
  );

  private CarpoolingRepository mockRepository;
  private CarpoolingService carpoolingService;
  private AreaStop boardingArea;
  private AreaStop alightingArea;
  private AreaStop farBoardingArea;

  @BeforeEach
  void setup() {
    mockRepository = mock(CarpoolingRepository.class);
    carpoolingService = new DefaultCarpoolingService(mockRepository);

    // Create area stops with realistic geometries
    boardingArea = createAreaStop("close-pickup", PICKUP_CENTER, 500); // 500m radius
    alightingArea = createAreaStop("close-dropoff", DROPOFF_CENTER, 500);
    farBoardingArea = createAreaStop("far-pickup", FAR_PICKUP_CENTER, 500);
  }

  @Test
  void route_withValidLocations_returnsEmptyItineraries() {
    // Given: A valid route request with coordinates
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: No carpool trips in repository
    when(mockRepository.getCarpoolTrips()).thenReturn(List.of());

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // Then: Empty list is returned (no available trips)
    assertTrue(result.isEmpty());
  }

  @Test
  void route_withCarpoolTripsWithinWalkingDistance_includesCandidates() {
    // Given: A valid route request
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Carpool trip with pickup/dropoff areas within walking distance
    CarpoolTrip nearbyTrip = new CarpoolTripBuilder(id("nearby-trip"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("nearby-trip").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(nearbyTrip));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // But the candidate should be processed (no exception thrown)
    assertEquals(1, result.size());
  }

  @Test
  void route_withCarpoolTripsTooFarAway_excludesCandidates() {
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Carpool trip with pickup area too far from origin
    CarpoolTrip farTrip = new CarpoolTripBuilder(id("far-trip"))
      .withBoardingArea(farBoardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("far-trip").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(farTrip));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // Then: No candidates are processed (too far away)
    assertTrue(result.isEmpty());
  }

  @Test
  void route_withMultipleCarpoolTrips_sortsAndLimitsCandidates() {
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Multiple carpool trips at different distances
    CarpoolTrip trip1 = createCarpoolTrip("trip-1", boardingArea, alightingArea);
    CarpoolTrip trip2 = createCarpoolTrip(
      "trip-2",
      createAreaStop("pickup-2", new WgsCoordinate(59.9180, 10.7450), 500),
      createAreaStop("dropoff-2", new WgsCoordinate(59.9480, 10.7580), 500)
    );
    CarpoolTrip trip3 = createCarpoolTrip(
      "trip-3",
      createAreaStop("pickup-3", new WgsCoordinate(59.9160, 10.7480), 500),
      createAreaStop("dropoff-3", new WgsCoordinate(59.9460, 10.7550), 500)
    );
    CarpoolTrip trip4 = createCarpoolTrip(
      "trip-4",
      createAreaStop("pickup-4", new WgsCoordinate(59.9140, 10.7500), 500),
      createAreaStop("dropoff-4", new WgsCoordinate(59.9440, 10.7570), 500)
    );

    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(trip1, trip2, trip3, trip4));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // The algorithm should limit to top 3 candidates and sort by cost
    assertEquals(3, result.size());
    assertEquals("F:trip-3", result.get(0).legs().getFirst().trip().getId().toString());
    assertEquals("F:trip-4", result.get(1).legs().getFirst().trip().getId().toString());
    assertEquals("F:trip-2", result.get(2).legs().getFirst().trip().getId().toString());
  }

  @Test
  void route_withTripsWithNullAreas_skipsInvalidTrips() {
    // Given: A valid route request
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(RoutingPreferences.of().build())
      .buildRequest();

    // And: Carpool trips with null pickup or dropoff areas
    CarpoolTrip tripWithNullPickup = new CarpoolTripBuilder(id("null-boarding"))
      .withBoardingArea(null)
      .withAlightingArea(alightingArea)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("null-boarding").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    CarpoolTrip tripWithNullDropoff = new CarpoolTripBuilder(id("null-alighting"))
      .withBoardingArea(boardingArea)
      .withAlightingArea(null)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TimetableRepositoryForTest.trip("null-alighting").build())
      .withProvider("TestProvider")
      .withAvailableSeats(2)
      .build();

    CarpoolTrip validTrip = createCarpoolTrip("valid-trip", boardingArea, alightingArea);

    when(mockRepository.getCarpoolTrips()).thenReturn(
      List.of(tripWithNullPickup, tripWithNullDropoff, validTrip)
    );

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // Then: Invalid trips are skipped, only valid trip is processed
    assertEquals(1, result.size());
  }

  @Test
  void route_withCustomWalkReluctance_adjustsEstimatedCost() {
    // Given: Route request with custom walk reluctance
    RoutingPreferences preferences = RoutingPreferences.of()
      .withWalk(wb -> wb.withReluctance(5.0)) // Higher reluctance
      .build();

    RouteRequest request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withDateTime(DATE_TIME)
      .withPreferences(preferences)
      .buildRequest();

    // And: Carpool trip within range
    CarpoolTrip trip = createCarpoolTrip("test-trip", boardingArea, alightingArea);
    when(mockRepository.getCarpoolTrips()).thenReturn(List.of(trip));

    // When: Routing is requested
    List<Itinerary> result = carpoolingService.route(request);

    // Then: Should process without error (cost calculation uses walk reluctance)
    assertEquals(1, result.size());
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
