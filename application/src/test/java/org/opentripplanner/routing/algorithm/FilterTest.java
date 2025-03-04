package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;

public class FilterTest {

  static final String AGENCY_ID_1 = "RUT:Agency:1";
  static final String AGENCY_ID_2 = "RUT:Agency:2";
  static final String AGENCY_ID_3 = "RUT:Agency:3";

  static final Agency AGENCY_1 = TimetableRepositoryForTest.agency("A")
    .copy()
    .withId(id(AGENCY_ID_1))
    .build();
  static final Agency AGENCY_2 = TimetableRepositoryForTest.agency("B")
    .copy()
    .withId(id(AGENCY_ID_2))
    .build();
  static final Agency AGENCY_3 = TimetableRepositoryForTest.agency("C")
    .copy()
    .withId(id(AGENCY_ID_3))
    .build();

  static final String ROUTE_ID_1 = "RUT:Route:1";
  static final String ROUTE_ID_2 = "RUT:Route:2";
  static final String ROUTE_ID_3 = "RUT:Route:3";
  static final String ROUTE_ID_4 = "RUT:Route:4";

  static final String JOURNEY_PATTERN_ID_1 = "RUT:JourneyPattern:1";
  static final String JOURNEY_PATTERN_ID_2 = "RUT:JourneyPattern:2";
  static final String JOURNEY_PATTERN_ID_3 = "RUT:JourneyPattern:3";
  static final String JOURNEY_PATTERN_ID_4 = "RUT:JourneyPattern:4";

  static final StopPattern STOP_PATTERN = TimetableRepositoryForTest.of().stopPattern(2);

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode NIGHT_BUS = SubMode.getOrBuildAndCacheForever("nightBus");

  final String GROUP_OF_Routes_ID_1 = "RUT:GroupOfLines:1";
  final String GROUP_OF_Routes_ID_2 = "RUT:GroupOfLines:2";

  final GroupOfRoutes GROUP_OF_ROUTES_1 = TimetableRepositoryForTest.groupOfRoutes(
    GROUP_OF_Routes_ID_1
  ).build();
  final GroupOfRoutes GROUP_OF_ROUTES_2 = TimetableRepositoryForTest.groupOfRoutes(
    GROUP_OF_Routes_ID_2
  ).build();

  @Test
  @DisplayName(
    """
    Filter test 1

    filters: [
      {
        select: [ {A} ]
      }
    ]

    -> A
    """
  )
  public void testOne() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).withAgency(AGENCY_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).withAgency(AGENCY_2).build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3).withAgency(AGENCY_3).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filterRequest = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_1))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(2, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_2)));
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 2

    filters: [
      {
        not: [ {A} ]
      }
    ]

    -> S - A
    """
  )
  public void testTwo() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).withAgency(AGENCY_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).withAgency(AGENCY_2).build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3).withAgency(AGENCY_3).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filterRequest = TransitFilterRequest.of()
      .addNot(SelectRequest.of().withAgencies(List.of(id(AGENCY_ID_1))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_1)));
  }

  @Test
  @DisplayName(
    """
    Filter test 3

    filters: [
      {
        select: [ {A}, {B} ]
      }
    ]

    -> A ∪ B
    """
  )
  public void testThree() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withAgency(AGENCY_1)
      .withMode(TransitMode.BUS)
      .withNetexSubmode("schoolBus")
      .build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withAgency(AGENCY_2)
      .withMode(TransitMode.RAIL)
      .withNetexSubmode("railShuttle")
      .build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3)
      .withAgency(AGENCY_3)
      .withMode(TransitMode.TRAM)
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filterRequest = TransitFilterRequest.of()
      .addSelect(
        SelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS, SubMode.of("schoolBus"))))
          .build()
      )
      .addSelect(
        SelectRequest.of()
          .withTransportModes(
            List.of(new MainAndSubMode(TransitMode.RAIL, SubMode.of("railShuttle")))
          )
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 4

    filters: [
      {
        select: [ {A} ]
      },
      {
        select: [ {B} ]
      }
    ]

    -> A ∪ B
    """
  )
  public void testFour() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter1 = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_1))).build())
      .build();

    var filter2 = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_2))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter1, filter2), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 5

    filters: [
      {
        select: [ {A} ]
      },
      {
        not: [ {B} ]
      }
    ]

    -> A ∪ (S - B)
    """
  )
  public void testFive() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter1 = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_1))).build())
      .build();

    var filter2 = TransitFilterRequest.of()
      .addNot(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_1))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter1, filter2), patterns);

    assertTrue(bannedPatterns.isEmpty());
  }

  @Test
  @DisplayName(
    """
    Filter test 6

    filters: [
      {
        select: [ {A} ]
        not: [ {B} ]
      }
    ]

    -> A - B
    """
  )
  public void testSix() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).withAgency(AGENCY_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).withAgency(AGENCY_1).build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3).withAgency(AGENCY_1).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filterRequest = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withAgencies(List.of(id(AGENCY_ID_1))).build())
      .addNot(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_3))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filterRequest), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 7

    filters: [
      {
        select: [ {A} ]
      },
      {
        select: [ {B} ]
        not: [ {C} ]
      }
    ]

    -> A ∪ (B - C)
    """
  )
  public void testSeven() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1).withAgency(AGENCY_1).build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2).withAgency(AGENCY_2).build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3).withAgency(AGENCY_2).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter1 = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withAgencies(List.of(id(AGENCY_ID_1))).build())
      .build();

    var filter2 = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withAgencies(List.of(id(AGENCY_ID_2))).build())
      .addNot(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_3))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter1, filter2), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 8

    filters: [
      {
        select: [ {A,B} ]
      }
    ]

    -> A ∩ B
    """
  )
  public void testEight() {
    final Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withMode(TransitMode.BUS)
      .withAgency(AGENCY_1)
      .build();
    final Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withMode(TransitMode.RAIL)
      .withAgency(AGENCY_1)
      .build();
    final Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3)
      .withMode(TransitMode.BUS)
      .withAgency(AGENCY_2)
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addSelect(
        SelectRequest.of()
          .withAgencies(List.of(id(AGENCY_ID_1)))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(2, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_2)));
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  @DisplayName(
    """
    Filter test 9

    filters: [
      {
        select: [ {A,B} ]
        not: [ {C} ]
      }
    ]

    -> (A ∩ B) - C
    """
  )
  public void testNine() {
    Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withAgency(AGENCY_1)
      .withMode(TransitMode.BUS)
      .build();
    Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withAgency(AGENCY_1)
      .withMode(TransitMode.RAIL)
      .build();
    Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3)
      .withAgency(AGENCY_1)
      .withMode(TransitMode.BUS)
      .build();
    Route route4 = TimetableRepositoryForTest.route(ROUTE_ID_4)
      .withAgency(AGENCY_2)
      .withMode(TransitMode.BUS)
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_4, route4)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addSelect(
        SelectRequest.of()
          .withAgencies(List.of(id(AGENCY_ID_1)))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .addNot(SelectRequest.of().withRoutes(List.of(id(ROUTE_ID_3))).build())
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(3, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_2)));
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_4)));
  }

  @Test
  @DisplayName(
    """
    Filter test 10

    filters: [
      {
        select: [ {A} ]
        not: [ {B, C} ]
      }
    ]

    -> A - (B ∩ C)
    """
  )
  public void testTen() {
    final Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withMode(TransitMode.BUS)
      .withAgency(AGENCY_1)
      .build();
    final Route route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withMode(TransitMode.RAIL)
      .withAgency(AGENCY_1)
      .build();
    final Route route3 = TimetableRepositoryForTest.route(ROUTE_ID_3)
      .withMode(TransitMode.BUS)
      .withAgency(AGENCY_1)
      .build();
    final Route route4 = TimetableRepositoryForTest.route(ROUTE_ID_4).withAgency(AGENCY_2).build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_3, route3)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_4, route4)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withAgencies(List.of(id(AGENCY_ID_1))).build())
      .addNot(
        SelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .withRoutes(List.of(id(ROUTE_ID_3)))
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(2, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_4)));
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_3)));
  }

  @Test
  void testDifferentSubModesInRoute() {
    final Route route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withMode(TransitMode.BUS)
      .withAgency(AGENCY_1)
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .withNetexSubmode(LOCAL_BUS)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route1)
        .withStopPattern(STOP_PATTERN)
        .withNetexSubmode(NIGHT_BUS)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addNot(
        SelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS, NIGHT_BUS)))
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_2)));
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

  @Test
  public void testGroupOfLinesSelectFunctionality() {
    var route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withGroupOfRoutes(List.of(GROUP_OF_ROUTES_1))
      .build();
    var route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withGroupOfRoutes(List.of(GROUP_OF_ROUTES_2))
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addSelect(
        SelectRequest.of()
          .withGroupOfRoutes(List.of(FeedScopedId.parse("F:" + GROUP_OF_Routes_ID_1)))
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_2)));
  }

  @Test
  public void testGroupOfLinesExcludeFunctionality() {
    var route1 = TimetableRepositoryForTest.route(ROUTE_ID_1)
      .withGroupOfRoutes(List.of(GROUP_OF_ROUTES_1))
      .build();
    var route2 = TimetableRepositoryForTest.route(ROUTE_ID_2)
      .withGroupOfRoutes(List.of(GROUP_OF_ROUTES_2))
      .build();

    var patterns = List.of(
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_1, route1)
        .withStopPattern(STOP_PATTERN)
        .build(),
      TimetableRepositoryForTest.tripPattern(JOURNEY_PATTERN_ID_2, route2)
        .withStopPattern(STOP_PATTERN)
        .build()
    );

    var filter = TransitFilterRequest.of()
      .addNot(
        SelectRequest.of()
          .withGroupOfRoutes(List.of(FeedScopedId.parse("F:" + GROUP_OF_Routes_ID_1)))
          .build()
      )
      .build();

    Collection<FeedScopedId> bannedPatterns = bannedPatterns(List.of(filter), patterns);

    assertEquals(1, bannedPatterns.size());
    assertTrue(bannedPatterns.contains(id(JOURNEY_PATTERN_ID_1)));
  }
}
