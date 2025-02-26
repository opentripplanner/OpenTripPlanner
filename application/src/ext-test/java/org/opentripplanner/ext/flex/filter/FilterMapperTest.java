package org.opentripplanner.ext.flex.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FilterMapperTest {

  private static final TripRequest ALLOW_ALL = TripRequest.of().build();
  public static final FeedScopedId ROUTE_ID1 = id("r1");
  public static final FeedScopedId AGENCY_ID1 = id("a1");

  @Test
  void allowAll() {
    var filter = FilterMapper.mapFilters(List.of(AllowAllTransitFilter.of()));
    assertEquals(ALLOW_ALL, filter);
  }

  @Test
  void routes() {
    var select = SelectRequest.of().withRoutes(List.of(ROUTE_ID1)).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var actual = FilterMapper.mapFilters(List.of(transitFilter));
    var expected = TripRequest
      .of()
      .withIncludedRoutes(FilterValues.ofEmptyIsNothing("includedRoutes", Set.of(ROUTE_ID1)))
      .withExcludedRoutes(FilterValues.ofEmptyIsEverything("excludedRoutes", Set.of(ROUTE_ID1)))
      .build();

    assertEquals(expected, actual);
  }

  @Test
  void agencies() {
    var select = SelectRequest.of().withAgencies(List.of(AGENCY_ID1)).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var actual = FilterMapper.mapFilters(List.of(transitFilter));
    var expected = TripRequest
      .of()
      .withIncludedAgencies(FilterValues.ofEmptyIsNothing("includedAgencies", Set.of(AGENCY_ID1)))
      .withExcludedAgencies(FilterValues.ofEmptyIsEverything("excludedAgencies", Set.of(AGENCY_ID1)))
      .build();

    assertEquals(expected, actual);
  }
}
