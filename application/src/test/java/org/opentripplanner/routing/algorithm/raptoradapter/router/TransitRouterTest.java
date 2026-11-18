package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheReport;

class TransitRouterTest {

  /**
   * A car-only request has the transfer mode CAR, which may have no transfer profile. No transit
   * search is done for it, so the regular transfers must not be resolved.
   */
  @Test
  void requestWithoutTransitDoesNotResolveTheRegularTransfers() {
    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(47.6564, 9.4740))
      .withTo(GenericLocation.fromCoordinate(49.4866, 8.4681))
      .withJourney(jb ->
        jb
          .withModes(RequestModes.of().withAllStreetModes(StreetMode.CAR).build())
          .withTransit(b -> b.disable())
      )
      .buildRequest();
    var result = TransitRouter.route(
      request,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      new FailingRegularTransfers(),
      null,
      List.of(),
      null,
      null,
      null,
      null,
      null,
      null,
      new DebugTimingAggregator(),
      null,
      null,
      null
    );

    assertTrue(result.getItineraries().isEmpty());
  }

  /** Regular transfers that fail the test if they are used. */
  private static class FailingRegularTransfers
    implements RegularTransferRepositorySnapshot<NearbyStop, RouteRequest>
  {

    @Override
    public RegularTransfersService<NearbyStop> forRequest(RouteRequest request) {
      return fail("The regular transfers of a request without transit are resolved");
    }

    @Override
    public void warmUp() {
      fail("Unexpected warm-up");
    }

    @Override
    public RegularTransferCacheReport cacheReport() {
      return fail("Unexpected cache report");
    }

    @Override
    public RegularTransferCacheMetrics cacheMetrics() {
      return fail("Unexpected cache metrics");
    }
  }
}
