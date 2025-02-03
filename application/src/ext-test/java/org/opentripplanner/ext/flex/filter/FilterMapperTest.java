package org.opentripplanner.ext.flex.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.filter.FlexTripFilterRequest.Filter;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;

class FilterMapperTest {

  @Test
  void allowAll() {
    var filter = FilterMapper.mapFilters(List.of(AllowAllTransitFilter.of()));
    assertEquals(FlexTripFilter.ALLOW_ALL, filter);
  }

  @Test
  void distinct() {
    var filter = FilterMapper.mapFilters(
      List.of(AllowAllTransitFilter.of(), AllowAllTransitFilter.of())
    );
    assertEquals(FlexTripFilter.ALLOW_ALL, filter);
  }

  @Test
  void routes() {
    var select = SelectRequest.of().withRoutes(List.of(id("r1"))).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var filter = FilterMapper.mapFilters(List.of(transitFilter));
    assertEquals(
      new FlexTripFilter(
        List.of(new Filter(Set.of(), Set.of(), Set.of(id("r1")), Set.of(id("r1"))))
      ),
      filter
    );
  }

  @Test
  void agencies() {
    var select = SelectRequest.of().withAgencies(List.of(id("a1"))).build();
    var transitFilter = TransitFilterRequest.of().addSelect(select).addNot(select).build();
    var filter = FilterMapper.mapFilters(List.of(transitFilter));
    assertEquals(
      new FlexTripFilter(
        List.of(new Filter(Set.of(id("a1")), Set.of(id("a1")), Set.of(), Set.of()))
      ),
      filter
    );
  }
}
