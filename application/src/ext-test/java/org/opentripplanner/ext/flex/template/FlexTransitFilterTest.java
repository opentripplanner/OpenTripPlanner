package org.opentripplanner.ext.flex.template;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

class FlexTransitFilterTest {

  private static List<Arguments> allowAllCases() {
    return List.of(
      of(route("r1"), agency("a1")),
      of(route("r1"), agency("a2")),
      of(route("r2"), agency("a2")),
      of(route("r3"), agency("a3"))
    );
  }

  @ParameterizedTest
  @MethodSource("allowAllCases")
  void allowAll(Route route, Agency agency) {
    var filter = FlexTransitFilter.ALLOW_ALL;

    var r = route.copy().withAgency(agency).build();
    var trip = TimetableRepositoryForTest.trip("1").withRoute(r).build();

    assertTrue(filter.allowsTrip(trip));
  }

  private static Route route(String routeId) {
    return TimetableRepositoryForTest.route(routeId).build();
  }
}
