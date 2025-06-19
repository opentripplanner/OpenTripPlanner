package org.opentripplanner.routing.algorithm.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.ViaLocationDeprecated;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponseConnection;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Create search from point A to point B via point C. Search will start at 12:00 and will find two
 * trips (itineraries) from A to C (s1i1, s1i2).
 * <p>
 * s1i1 arrives at C 13:00 and s1i2 arrives at C 14:00
 * <p>
 * minSlack is 10 min and maxSlack is 45 minutes.
 * <p>
 * Search from C to B at 13:15 (first arrival + minSlack) gives 3 results s2i1, s2i2 and s2i3. s2i1
 * departures at 13:15, s2i2 departures at 13:45 and s2i3 departures at 14:30
 * <p>
 * s1i1 should be combined with s2i1 and s2i2. Not s2i3 because maxSlack gives that it shold not
 * match departures after 13:45 (first arrival + maxSlack).
 * <p>
 * s1i2 should be combined only with s2i3 because it arrives after departure on the others.
 */
public class ViaRoutingWorkerTest {

  private static Itinerary s1i1;
  private static Itinerary s1i2;
  private static Itinerary s2i1;
  private static Itinerary s2i2;
  private static Itinerary s2i3;

  private static List<Itinerary> firstSearch;
  private static List<Itinerary> secondSearch;

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private final Place fromA = testModel.place("A", 5.0, 8.0);
  private final Place viaC = testModel.place("C", 7.0, 9.0);
  private final Place toB = testModel.place("B", 6.0, 8.5);

  @Test
  public void testViaRoutingWorker() {
    // Prepare test
    createItinieraries();
    var request = createRouteViaRequest();

    var result = new ViaRoutingWorker(request, this::createRoutingResponse).route();

    assertNotNull(result, "result must not be null");
    assertNotNull(result.plan(), "plan must not be null");

    var plan = result.plan();

    assertFalse(plan.keySet().isEmpty(), "plan must not be empty");
    assertEquals(2, plan.keySet().size(), "plan should contain 2 itineraries");

    // First trip (s1s1) should match 2 trips (s2i1, s2i2)
    assertNotNull(result.plan().get(s1i1), "First itinerary (s1i1) should be in the plan");
    assertEquals(
      2,
      result.plan().get(s1i1).size(),
      "First itinerary should be combined with two itineraries on second search"
    );
    assertTrue(
      result.plan().get(s1i1).contains(s2i1),
      "First itinerary should be combined with s2i1"
    );
    assertTrue(
      result.plan().get(s1i1).contains(s2i2),
      "First itinerary should be combined with s2i2"
    );

    // Second trip (s1s2) should match 1 trip (s2r3)
    assertNotNull(result.plan().get(s1i2), "Second itinerary (s1i2) should be in the plan");
    assertEquals(
      1,
      result.plan().get(s1i2).size(),
      "Second itinerary should be combined with one itinerary on second search"
    );
    assertTrue(
      result.plan().get(s1i2).contains(s2i3),
      "Second itinerary should be combined with s2i3"
    );

    // Assert that the connection creation logic works and returns the same combinations as above
    assertEquals(
      List.of(
        List.of(
          new ViaRoutingResponseConnection(0, 0),
          new ViaRoutingResponseConnection(0, 1),
          new ViaRoutingResponseConnection(1, 2)
        )
      ),
      result.createConnections()
    );
  }

  /**
   * This function simulates the RoutingWorker for each request
   */
  private RoutingResponse createRoutingResponse(RouteRequest req) {
    // request from A or C?
    var c = fromA.coordinate;
    var firstOrSecondSearch = req.from().lng == c.longitude() && req.from().lat == c.latitude();

    var searchItineraries = firstOrSecondSearch ? firstSearch : secondSearch;

    var tripPlan = new TripPlan(null, null, null, searchItineraries);
    return new RoutingResponse(tripPlan, null, null, null, null, null);
  }

  /**
   * From A to B via C at 12:00 with minSlack and maxSlack as described.
   */
  public RouteViaRequest createRouteViaRequest() {
    var dateTime = ZonedDateTime.parse("2021-12-02T12:00:00-05:00[America/New_York]").toInstant();
    // One via location and 2 viaJourneys
    int minSlack = 10;
    int maxSlack = 45;
    var viaLocations = List.of(
      new ViaLocationDeprecated(
        location(viaC),
        false,
        Duration.ofMinutes(minSlack),
        Duration.ofMinutes(maxSlack)
      )
    );

    var viaJourneys = List.of(JourneyRequest.of().build(), JourneyRequest.of().build());

    return RouteViaRequest.of(viaLocations, viaJourneys)
      .withDateTime(dateTime)
      .withFrom(location(fromA))
      .withTo(location(toB))
      .withSearchWindow(Duration.ofHours(1))
      .build();
  }

  /**
   * Create itinerary as described in class documentation.
   */
  void createItinieraries() {
    // Arrive 13
    s1i1 = newItinerary(fromA)
      .bus(1, TimeUtils.hm2time(12, 0), TimeUtils.hm2time(13, 0), viaC)
      .build();
    // Arrive 14
    s1i2 = newItinerary(fromA)
      .bus(2, TimeUtils.hm2time(12, 0), TimeUtils.hm2time(14, 0), viaC)
      .build();

    // departure 13:15
    s2i1 = newItinerary(viaC)
      .bus(3, TimeUtils.hm2time(13, 15), TimeUtils.hm2time(15, 0), toB)
      .build();
    // departure 13:45
    s2i2 = newItinerary(viaC)
      .bus(3, TimeUtils.hm2time(13, 45), TimeUtils.hm2time(15, 0), toB)
      .build();
    // departure 14:30
    s2i3 = newItinerary(viaC)
      .bus(3, TimeUtils.hm2time(14, 30), TimeUtils.hm2time(15, 0), toB)
      .build();

    firstSearch = List.of(s1i1, s1i2);
    secondSearch = List.of(s2i1, s2i2, s2i3);
  }

  private static GenericLocation location(Place p) {
    return GenericLocation.fromCoordinate(p.coordinate.latitude(), p.coordinate.longitude());
  }
}
