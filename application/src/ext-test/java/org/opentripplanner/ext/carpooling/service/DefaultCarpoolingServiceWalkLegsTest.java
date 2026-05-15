package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Integration tests that exercise the walk-to/from-carpool behavior added to
 * {@link DefaultCarpoolingService}. The graph places the passenger's origin and destination on
 * pedestrian-only edges, so the snapper must find a nearby car-accessible vertex and the
 * resulting itinerary must contain leading and trailing WALK {@link StreetLeg}s around the
 * carpool leg.
 *
 * <pre>
 *   A ====== B ============= C ====== D          (=  biStreet: car + ped)
 *            .               .
 *            . (ped only)    . (ped only)
 *            P               Q                   (passenger pickup / dropoff)
 * </pre>
 *
 * <ul>
 *   <li>{@code A} — carpool trip origin (where the driver starts).
 *   <li>{@code D} — carpool trip destination (where the driver ends).
 *   <li>{@code B} — drivable mid-route intersection nearest to the passenger's origin; the snapper
 *       resolves it as the car-accessible pickup vertex because {@code P} sits on a
 *       pedestrian-only side branch the car cannot enter.
 *   <li>{@code C} — drivable mid-route intersection nearest to the passenger's destination; the
 *       snapper resolves it as the car-accessible dropoff vertex for the same reason.
 *   <li>{@code P} — passenger origin, off the drivable network on a pedestrian-only side branch
 *       from B.
 *   <li>{@code Q} — passenger destination, off the drivable network on a pedestrian-only side
 *       branch from C.
 * </ul>
 *
 * The expected itinerary therefore walks {@code P → B}, drives {@code B → C} as a carpool leg,
 * and walks {@code C → Q}.
 */
class DefaultCarpoolingServiceWalkLegsTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final WgsCoordinate TRIP_START = ORIGIN;
  private static final WgsCoordinate B_COORD = ORIGIN.moveEastMeters(500);
  private static final WgsCoordinate C_COORD = ORIGIN.moveEastMeters(1500);
  private static final WgsCoordinate TRIP_END = ORIGIN.moveEastMeters(2000);
  private static final WgsCoordinate PASSENGER_REQUESTED_PICKUP = B_COORD.moveSouthMeters(80);
  private static final WgsCoordinate PASSENGER_REQUESTED_DROPOFF = C_COORD.moveSouthMeters(80);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  private DefaultCarpoolingService service;
  private CarpoolingRepository repository;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", TRIP_START);
          var B = intersection("B", B_COORD);
          var C = intersection("C", C_COORD);
          var D = intersection("D", TRIP_END);

          var P = intersection("P", PASSENGER_REQUESTED_PICKUP);
          var Q = intersection("Q", PASSENGER_REQUESTED_DROPOFF);

          biStreet(A, B, 500);
          biStreet(B, C, 1000);
          biStreet(C, D, 500);
          // Passenger pickup and dropoff sit on pedestrian-only side branches off the main road.
          street(
            B,
            P,
            80,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
          street(
            C,
            Q,
            80,
            StreetTraversalPermission.PEDESTRIAN,
            StreetTraversalPermission.PEDESTRIAN
          );
        }
      }
    );

    Graph graph = model.graph();
    var timetableRepository = model.timetableRepository();
    VertexLinker vertexLinker = VertexLinkerTestFactory.of(graph);
    var vertexCreationService = new VertexCreationService(vertexLinker);
    TransitService transitService = new DefaultTransitService(timetableRepository);
    repository = new DefaultCarpoolingRepository();

    StreetLimitationParametersService streetLimitationParams =
      new StreetLimitationParametersService() {
        @Override
        public float maxCarSpeed() {
          return 40.0f;
        }

        @Override
        public int maxAreaNodes() {
          return 500;
        }
      };

    service = new DefaultCarpoolingService(
      repository,
      streetLimitationParams,
      transitService,
      vertexCreationService
    );
  }

  private RouteRequest buildDirectCarpoolRequest(ZonedDateTime dateTime) {
    return RouteRequest.of()
      .withFrom(
        GenericLocation.fromCoordinate(
          PASSENGER_REQUESTED_PICKUP.latitude(),
          PASSENGER_REQUESTED_PICKUP.longitude()
        )
      )
      .withTo(
        GenericLocation.fromCoordinate(
          PASSENGER_REQUESTED_DROPOFF.latitude(),
          PASSENGER_REQUESTED_DROPOFF.longitude()
        )
      )
      .withDateTime(dateTime.toInstant())
      .withJourney(j -> j.withDirect(new StreetRequest(StreetMode.CARPOOL)))
      .buildRequest();
  }

  @Test
  void passengerOnPedestrianOnlyEdge_emitsWalkLegsAroundCarpoolLeg() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(TRIP_START, TRIP_END, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildDirectCarpoolRequest(SEARCH_TIME);
    var results = service.routeDirect(request);

    assertFalse(results.isEmpty(), "Expected a direct carpool itinerary when walks are feasible");

    for (var itinerary : results) {
      var legs = itinerary.legs();
      assertEquals(
        3,
        legs.size(),
        () -> "Expected WALK + CARPOOL + WALK, got " + legs.size() + " legs: " + legs
      );

      var walkToPickup = legs.getFirst();
      var carpoolLeg = legs.get(1);
      assertTrue(
        walkToPickup instanceof StreetLeg sl && sl.getMode() == TraverseMode.WALK,
        "First leg should be a walking StreetLeg"
      );
      assertTrue(
        walkToPickup.duration().toSeconds() > 0,
        "Walk-to-pickup duration should be positive"
      );
      // Time anchoring: the walk to the pickup must end exactly when the carpool boards, and
      // start exactly walk-duration before that. This pins down both the chaining and the
      // back-shift performed by CarpoolItineraryMapper.buildItinerary.
      assertEquals(
        carpoolLeg.startTime(),
        walkToPickup.endTime(),
        "Walk-to-pickup should end exactly when the carpool leg starts"
      );
      assertEquals(
        carpoolLeg.startTime().minus(walkToPickup.duration()),
        walkToPickup.startTime(),
        "Walk-to-pickup should start one walk-duration before the carpool leg starts"
      );
      assertTrue(
        walkToPickup.from().coordinate.sameLocation(PASSENGER_REQUESTED_PICKUP),
        () ->
          "Walk-to-pickup should start at the passenger origin (P) " +
          PASSENGER_REQUESTED_PICKUP +
          " but started at " +
          walkToPickup.from().coordinate
      );
      assertTrue(
        walkToPickup.to().coordinate.sameLocation(B_COORD),
        () ->
          "Walk-to-pickup should end at the snapped pickup vertex B " +
          B_COORD +
          " but ended at " +
          walkToPickup.to().coordinate
      );

      assertTrue(
        carpoolLeg instanceof CarpoolLeg,
        () -> "Middle leg should be a CarpoolLeg representing the driving share, was " + carpoolLeg
      );
      assertEquals(
        TransitMode.CARPOOL,
        ((CarpoolLeg) carpoolLeg).mode(),
        "Middle leg mode should be TransitMode.CARPOOL (driving)"
      );
      assertTrue(
        carpoolLeg.from().coordinate.sameLocation(B_COORD),
        () ->
          "Carpool leg should board at vertex B " +
          B_COORD +
          " but boarded at " +
          carpoolLeg.from().coordinate
      );
      assertTrue(
        carpoolLeg.to().coordinate.sameLocation(C_COORD),
        () ->
          "Carpool leg should alight at vertex C " +
          C_COORD +
          " but alighted at " +
          carpoolLeg.to().coordinate
      );

      var walkFromDropoff = legs.getLast();
      assertTrue(
        walkFromDropoff instanceof StreetLeg sl && sl.getMode() == TraverseMode.WALK,
        "Last leg should be a walking StreetLeg"
      );
      assertTrue(
        walkFromDropoff.duration().toSeconds() > 0,
        "Walk-from-dropoff duration should be positive"
      );
      assertEquals(
        carpoolLeg.endTime(),
        walkFromDropoff.startTime(),
        "Walk-from-dropoff should start exactly when the carpool leg ends"
      );
      assertEquals(
        carpoolLeg.endTime().plus(walkFromDropoff.duration()),
        walkFromDropoff.endTime(),
        "Walk-from-dropoff should end one walk-duration after the carpool leg ends"
      );
      assertTrue(
        walkFromDropoff.from().coordinate.sameLocation(C_COORD),
        () ->
          "Walk-from-dropoff should start at the snapped dropoff vertex C " +
          C_COORD +
          " but started at " +
          walkFromDropoff.from().coordinate
      );
      assertTrue(
        walkFromDropoff.to().coordinate.sameLocation(PASSENGER_REQUESTED_DROPOFF),
        () ->
          "Walk-from-dropoff should end at the passenger destination (Q) " +
          PASSENGER_REQUESTED_DROPOFF +
          " but ended at " +
          walkFromDropoff.to().coordinate
      );
    }
  }
}
