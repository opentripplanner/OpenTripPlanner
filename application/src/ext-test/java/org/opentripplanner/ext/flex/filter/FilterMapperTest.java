package org.opentripplanner.ext.flex.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FilterMapperTest {

  private static final TripRequest ALLOW_ALL = TripRequest.of().build();
  public static final FeedScopedId ROUTE_ID1 = id("r1");
  public static final FeedScopedId AGENCY_ID1 = id("a1");

  @Test
  void allowAll() {
    var filter = FilterMapper.map(List.of(AllowAllTransitFilter.of()));
    assertEquals(ALLOW_ALL, filter);
  }

  @Test
  void routes() {
    var select = SelectRequest.of().withRoutes(List.of(ROUTE_ID1)).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var actual = FilterMapper.map(List.of(transitFilter));
    var expected = TripRequest.of()
      .withIncludeRoutes(List.of(ROUTE_ID1))
      .withExcludeRoutes(List.of(ROUTE_ID1))
      .build();

    assertEquals(expected, actual);
  }

  @Test
  void agencies() {
    var select = SelectRequest.of().withAgencies(List.of(AGENCY_ID1)).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var actual = FilterMapper.map(List.of(transitFilter));
    var expected = TripRequest.of()
      .withIncludeAgencies(List.of(AGENCY_ID1))
      .withExcludeAgencies(List.of(AGENCY_ID1))
      .build();

    assertEquals(expected, actual);
  }

  @Test
  void combinations() {
    var selectRoutes = SelectRequest.of().withRoutes(List.of(ROUTE_ID1)).build();
    var routeFilter = TransitFilterRequest.of()
      .addSelect(selectRoutes)
      .addNot(selectRoutes)
      .build();
    var selectAgencies = SelectRequest.of().withAgencies(List.of(AGENCY_ID1)).build();
    var agencyFilter = TransitFilterRequest.of()
      .addSelect(selectAgencies)
      .addNot(selectAgencies)
      .build();

    var actual = FilterMapper.map(List.of(routeFilter, agencyFilter));
    var expected = TripRequest.of()
      .withIncludeAgencies(List.of(AGENCY_ID1))
      .withExcludeAgencies(List.of(AGENCY_ID1))
      .withIncludeRoutes(List.of(ROUTE_ID1))
      .withExcludeRoutes(List.of(ROUTE_ID1))
      .build();

    assertEquals(expected, actual);

    assertEquals(
      "TripRequest{includeAgencies: NullIsEverythingFilter{name: 'includeAgencies', values: [F:a1]}, includeRoutes: NullIsEverythingFilter{name: 'includeRoutes', values: [F:r1]}, excludeAgencies: EmptyIsEverythingFilter{name: 'excludedAgencies', values: [F:a1]}, excludeRoutes: EmptyIsEverythingFilter{name: 'excludedRoutes', values: [F:r1]}, includeNetexInternalPlanningCodes: NullIsEverythingFilter{name: 'includeNetexInternalPlanningCodes'}, includeServiceDates: NullIsEverythingFilter{name: 'includeServiceDates'}}",
      actual.toString()
    );
  }
}
