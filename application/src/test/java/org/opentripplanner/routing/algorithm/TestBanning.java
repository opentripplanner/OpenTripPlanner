package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Test the banning and whitelisting functionality in the RouteRequest.
 * TODO This does not test the that banning/whitelisting affects the routing correctly.
 */
public class TestBanning {

  @Test
  public void testSetBannedOnRequest() {
    Collection<TripPattern> patterns = getTestPatterns();

    var filterRequest = TransitFilterRequest.of()
      .addNot(SelectRequest.of().withRoutes(List.of(id("RUT:Route:1"))).build())
      .addNot(SelectRequest.of().withAgencies(List.of(id("RUT:Agency:2"))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(2, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id("RUT:JourneyPattern:1")));
    assertTrue(bannedPatterns.contains(id("RUT:JourneyPattern:3")));
  }

  @Test
  public void testSetWhiteListedOnRequest() {
    Collection<TripPattern> patterns = getTestPatterns();

    var filterRequest = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withRoutes(List.of(id("RUT:Route:1"))).build())
      .addSelect(SelectRequest.of().withAgencies(List.of(id("RUT:Agency:2"))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id("RUT:JourneyPattern:2")));
  }

  private List<TripPattern> getTestPatterns() {
    Agency agency1 = TimetableRepositoryForTest.agency("A")
      .copy()
      .withId(id("RUT:Agency:1"))
      .build();
    Agency agency2 = TimetableRepositoryForTest.agency("B")
      .copy()
      .withId(id("RUT:Agency:2"))
      .build();

    Route route1 = TimetableRepositoryForTest.route("RUT:Route:1").withAgency(agency1).build();
    Route route2 = TimetableRepositoryForTest.route("RUT:Route:2").withAgency(agency1).build();
    Route route3 = TimetableRepositoryForTest.route("RUT:Route:3").withAgency(agency2).build();

    final StopPattern stopPattern = TimetableRepositoryForTest.of().stopPattern(2);
    return List.of(
      TimetableRepositoryForTest.tripPattern("RUT:JourneyPattern:1", route1)
        .withStopPattern(stopPattern)
        .build(),
      TimetableRepositoryForTest.tripPattern("RUT:JourneyPattern:2", route2)
        .withStopPattern(stopPattern)
        .build(),
      TimetableRepositoryForTest.tripPattern("RUT:JourneyPattern:3", route3)
        .withStopPattern(stopPattern)
        .build()
    );
  }

  private static Collection<FeedScopedId> bannedPatterns(
    List<TransitFilterRequest> filterRequest,
    Collection<TripPattern> patterns
  ) {
    return patterns
      .stream()
      .filter(pattern ->
        filterRequest.stream().noneMatch(filter -> filter.matchTripPattern(pattern))
      )
      .map(TripPattern::getId)
      .toList();
  }
}
