package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;

class DirectTransitRequestMapperTest {

  @Test
  void noDirectTransitForViaPoints() {
    var req = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withViaLocations(List.of(new VisitViaLocation(null, null, List.of(id("stopX")), null)))
      .buildRequest();
    var params = new RaptorRequestBuilder<>().searchParams().buildSearchParam();

    // We don't have any support for via points in the direct transit search.
    // Until we do the direct transit search should be disabled if there are via points
    assertEquals(Optional.empty(), DirectTransitRequestMapper.map(req, params));
  }

  @Test
  void mapSearchTimes() {
    var req = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1))
      .withPreferences(p ->
        p.withTransit(t -> t.withDirectTransitPreferences(d -> d.withEnabled(true)))
      )
      .buildRequest();
    var params = new RaptorRequestBuilder<>()
      .searchParams()
      .earliestDepartureTime(1000)
      .latestArrivalTime(5000)
      .searchWindowInSeconds(600)
      .addAccessPaths(TestAccessEgress.walk(1, 60))
      .addEgressPaths(TestAccessEgress.walk(2, 60))
      .buildSearchParam();

    var result = DirectTransitRequestMapper.map(req, params).orElseThrow();

    assertEquals(1000, result.earliestDepartureTime());
    assertEquals(5000, result.latestArrivalTime());
    assertEquals(600, result.searchWindowInSeconds());
  }
}
