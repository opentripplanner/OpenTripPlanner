package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;
import static org.opentripplanner.routing.api.request.StreetMode.NOT_SET;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Tests for planning with intermediate places
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Disabled
public class TestIntermediatePlaces {

  /**
   * The spatial deviation that we allow in degrees
   */
  public static final double DELTA = 0.005;

  private static ZoneId timeZone;

  private static GraphPathFinder graphPathFinder;

  private static Graph graph;

  private static GraphPathToItineraryMapper graphPathToItineraryMapper;

  @BeforeAll
  public static void setUp() {
    try {
      TestOtpModel model = FakeGraph.buildGraphNoTransit();
      graph = model.graph();
      TransitModel transitModel = model.transitModel();
      FakeGraph.addPerpendicularRoutes(graph, transitModel);
      FakeGraph.link(graph, transitModel);
      model.index();
      TestIntermediatePlaces.graphPathFinder = new GraphPathFinder(null);
      timeZone = transitModel.getTimeZone();

      graphPathToItineraryMapper =
        new GraphPathToItineraryMapper(
          timeZone,
          graph.streetNotesService,
          graph.ellipsoidToGeoidDifference
        );
    } catch (Exception e) {
      e.printStackTrace();
      assert false : "Could not add transit data: " + e.toString();
    }
  }

  @Test
  public void testWithoutIntermediatePlaces() {
    GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
    GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
    GenericLocation[] intermediateLocations = {};

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().build(),
      List.of(ExcludeAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().build(),
      List.of(ExcludeAllTransitFilter.of()),
      true
    );
  }

  @Test
  @Disabled
  public void testOneIntermediatePlace() {
    GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
    GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
    GenericLocation[] intermediateLocations = { new GenericLocation(39.92099, -82.95570) };

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().build(),
      List.of(ExcludeAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().build(),
      List.of(ExcludeAllTransitFilter.of()),
      true
    );
  }

  @Test
  @Disabled
  public void testTwoIntermediatePlaces() {
    GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
    GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
    GenericLocation[] intermediateLocations = new GenericLocation[2];
    intermediateLocations[0] = new GenericLocation(39.92099, -82.95570);
    intermediateLocations[1] = new GenericLocation(39.96146, -82.99552);

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().withDirectMode(CAR).build(),
      List.of(ExcludeAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().withDirectMode(CAR).build(),
      List.of(ExcludeAllTransitFilter.of()),
      true
    );
  }

  @Test
  public void testTransitWithoutIntermediatePlaces() {
    GenericLocation fromLocation = new GenericLocation(39.9308, -83.0118);
    GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
    GenericLocation[] intermediateLocations = {};

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      true
    );
  }

  @Test
  public void testThreeBusStopPlaces() {
    GenericLocation fromLocation = new GenericLocation(39.9058, -83.1341);
    GenericLocation toLocation = new GenericLocation(39.9058, -82.8841);
    GenericLocation[] intermediateLocations = { new GenericLocation(39.9058, -82.9841) };

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().withDirectMode(NOT_SET).build(),
      List.of(AllowAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.of().withDirectMode(NOT_SET).build(),
      List.of(AllowAllTransitFilter.of()),
      true
    );
  }

  @Test
  public void testTransitOneIntermediatePlace() {
    GenericLocation fromLocation = new GenericLocation(39.9108, -83.0118);
    GenericLocation toLocation = new GenericLocation(39.9698, -83.0198);
    GenericLocation[] intermediateLocations = { new GenericLocation(39.9948, -83.0148) };

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      true
    );
  }

  @Test
  public void testTransitTwoIntermediatePlaces() {
    GenericLocation fromLocation = new GenericLocation(39.9908, -83.0118);
    GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
    GenericLocation[] intermediateLocations = new GenericLocation[2];
    intermediateLocations[0] = new GenericLocation(40.0000, -82.900);
    intermediateLocations[1] = new GenericLocation(39.9100, -83.100);

    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      false
    );
    handleRequest(
      fromLocation,
      toLocation,
      intermediateLocations,
      RequestModes.defaultRequestModes(),
      List.of(AllowAllTransitFilter.of()),
      true
    );
  }

  private void handleRequest(
    GenericLocation from,
    GenericLocation to,
    GenericLocation[] via,
    RequestModes modes,
    List<TransitFilter> filters,
    boolean arriveBy
  ) {
    RouteRequest request = new RouteRequest();
    request.journey().setModes(modes);
    request.journey().transit().setFilters(filters);
    request.setDateTime("2016-04-20", "13:00", timeZone);
    request.setArriveBy(arriveBy);
    request.setFrom(from);
    request.setTo(to);
    for (GenericLocation intermediateLocation : via) {
      // TODO VIA - Replace with a RouteViaRequest
      //options.addIntermediatePlace(intermediateLocation);
    }
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        request,
        request.journey().access().mode(),
        request.journey().egress().mode()
      )
    ) {
      List<GraphPath<State, Edge, Vertex>> paths = graphPathFinder.graphPathFinderEntryPoint(
        request,
        temporaryVertices
      );

      assertNotNull(paths);
      assertFalse(paths.isEmpty());
      List<Itinerary> itineraries = graphPathToItineraryMapper.mapItineraries(paths);
      TripPlan plan = TripPlanMapper.mapTripPlan(request, itineraries);
      assertLocationIsVeryCloseToPlace(from, plan.from);
      assertLocationIsVeryCloseToPlace(to, plan.to);
      assertTrue(1 <= plan.itineraries.size());
      for (Itinerary itinerary : plan.itineraries) {
        validateIntermediatePlacesVisited(itinerary, via);
        assertTrue(via.length < itinerary.getLegs().size());
        validateLegsTemporally(request, itinerary);
        validateLegsSpatially(plan, itinerary);

        // technically this is not 100% right but should work of a test
        if (!filters.contains(ExcludeAllTransitFilter.of())) {
          assertTrue(itinerary.getTransitDuration().toSeconds() > 0);
        }
      }
    }
  }

  // Check that every via location is visited in the right order
  private void validateIntermediatePlacesVisited(Itinerary itinerary, GenericLocation[] via) {
    int legIndex = 0;

    for (GenericLocation location : via) {
      Leg leg;
      do {
        assertTrue(
          legIndex < itinerary.getLegs().size(),
          "Intermediate location was not an endpoint of any leg"
        );
        leg = itinerary.getLegs().get(legIndex);
        legIndex++;
      } while (
        Math.abs(leg.getTo().coordinate.latitude() - location.lat) > DELTA ||
        Math.abs(leg.getTo().coordinate.longitude() - location.lng) > DELTA
      );
    }
  }

  // Check that the end point of a leg is also the start point of the next leg
  private void validateLegsSpatially(TripPlan plan, Itinerary itinerary) {
    Place place = plan.from;
    for (Leg leg : itinerary.getLegs()) {
      assertEquals(place.coordinate, leg.getFrom().coordinate);
      place = leg.getTo();
    }
    assertEquals(place.coordinate, plan.to.coordinate);
  }

  // Check that the start time and end time of each leg are consistent
  private void validateLegsTemporally(RouteRequest request, Itinerary itinerary) {
    Instant departTime;
    Instant arriveTime;
    if (request.arriveBy()) {
      departTime = itinerary.getLegs().get(0).getStartTime().toInstant();
      arriveTime = request.dateTime();
    } else {
      departTime = request.dateTime();
      arriveTime = itinerary.getLegs().get(itinerary.getLegs().size() - 1).getEndTime().toInstant();
    }
    long sumOfDuration = 0;
    for (Leg leg : itinerary.getLegs()) {
      assertFalse(departTime.isAfter(leg.getStartTime().toInstant()));
      assertFalse(leg.getStartTime().isAfter(leg.getEndTime()));

      departTime = leg.getEndTime().toInstant();
      sumOfDuration += leg.getDuration().toSeconds();
    }
    sumOfDuration += itinerary.getWaitingDuration().toSeconds();

    assertFalse(departTime.isAfter(arriveTime));

    // Check the total duration of the legs,
    int accuracy = itinerary.getLegs().size(); // allow 1 second per leg for rounding errors
    assertEquals(sumOfDuration, itinerary.getDuration().toSeconds(), accuracy);
  }

  private void assertLocationIsVeryCloseToPlace(GenericLocation location, Place place) {
    assertEquals(location.lat, place.coordinate.latitude(), DELTA);
    assertEquals(location.lng, place.coordinate.longitude(), DELTA);
  }
}
