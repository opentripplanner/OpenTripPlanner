package org.opentripplanner.routing.algorithm;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.CarpoolAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.DefaultAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Unit tests for {@link RoutingWorker#selectRouters()} and
 * {@link RoutingWorker#selectAccessEgressRouters(StreetMode)}, the two places where all
 * mode-equality and feature-flag checks for router selection live.
 */
class RoutingWorkerTest {

  private static final GenericLocation FROM = GenericLocation.fromCoordinate(60.0, 10.0);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(59.0, 12.0);
  private static final GenericLocation ON_BOARD_FROM = GenericLocation.fromTripLocation(
    TripLocation.of(
      TripOnDateReference.ofTripOnServiceDateId(new FeedScopedId("F", "trip-1")),
      new FeedScopedId("F", "stop-1")
    )
  );

  private static RouteRequestBuilder baseRequest() {
    return RouteRequest.of().withFrom(FROM).withTo(TO);
  }

  private static RoutingWorker workerFor(RouteRequest request) {
    var workerRequest = new RoutingWorkerRequest(
      request,
      ZonedDateTime.now(ZoneOffset.UTC),
      AdditionalSearchDays.defaults(ZonedDateTime.now(ZoneOffset.UTC))
    );
    @SuppressWarnings("unchecked")
    RaptorConfig<TripSchedule> raptorConfig = mock(RaptorConfig.class);
    return new RoutingWorker(
      mock(TransitService.class),
      mock(TransitAlertService.class),
      mock(Graph.class),
      raptorConfig,
      new SimpleMeterRegistry(),
      mock(StreetLimitationParametersService.class),
      mock(VehicleRentalService.class),
      mock(StreetDetailsService.class),
      mock(RegularTransferService.class),
      FlexParameters.defaultValues(),
      List.<RideHailingService>of(),
      null,
      null,
      mock(ViaCoordinateTransferFactory.class),
      mock(CarpoolingService.class),
      null,
      null,
      mock(LinkingContextFactory.class),
      mock(TransitTuningParameters.class),
      mock(RaptorTuningParameters.class),
      workerRequest
    );
  }

  // --- selectAccessEgressRouters(StreetMode) ---

  @Test
  void walkAccessEgressUsesOnlyDefaultRouter() {
    var worker = workerFor(baseRequest().buildRequest());

    var routers = worker.selectAccessEgressRouters(StreetMode.WALK);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void flexibleAccessEgressWithFlexRoutingOffUsesOnlyDefaultRouter() {
    var worker = workerFor(baseRequest().buildRequest());

    var routers = worker.selectAccessEgressRouters(StreetMode.FLEXIBLE);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void flexibleAccessEgressWithFlexRoutingOnAddsFlexRouter() {
    var worker = workerFor(baseRequest().buildRequest());

    OTPFeature.FlexRouting.testOn(() -> {
      var routers = worker.selectAccessEgressRouters(StreetMode.FLEXIBLE);

      assertThat(routers).hasSize(2);
      assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
      assertThat(routers.get(1)).isInstanceOf(FlexAccessEgressRouter.class);
    });
  }

  @Test
  void carpoolAccessEgressWithCarPoolingOffUsesOnlyDefaultRouter() {
    var worker = workerFor(baseRequest().buildRequest());

    var routers = worker.selectAccessEgressRouters(StreetMode.CARPOOL);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void carpoolAccessEgressWithCarPoolingOnAddsCarpoolRouter() {
    var worker = workerFor(baseRequest().buildRequest());

    OTPFeature.CarPooling.testOn(() -> {
      var routers = worker.selectAccessEgressRouters(StreetMode.CARPOOL);

      assertThat(routers).hasSize(2);
      assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
      assertThat(routers.get(1)).isInstanceOf(CarpoolAccessEgressRouter.class);
    });
  }

  // --- selectRouters() ---

  @Test
  void defaultRequestSelectsStreetAndTransit() {
    var worker = workerFor(baseRequest().buildRequest());

    assertThat(worker.selectRouters()).hasSize(2);
  }

  @Test
  void flexDirectModeWithFlexRoutingOffSkipsFlexRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.FLEXIBLE)))
      .buildRequest();
    var worker = workerFor(request);

    assertThat(worker.selectRouters()).hasSize(2);
  }

  @Test
  void flexDirectModeWithFlexRoutingOnAddsFlexRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.FLEXIBLE)))
      .buildRequest();
    var worker = workerFor(request);

    OTPFeature.FlexRouting.testOn(() -> assertThat(worker.selectRouters()).hasSize(3));
  }

  @Test
  void carpoolDirectModeWithCarPoolingOnAddsCarpoolRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.CARPOOL)))
      .buildRequest();
    var worker = workerFor(request);

    OTPFeature.CarPooling.testOn(() -> assertThat(worker.selectRouters()).hasSize(3));
  }

  @Test
  void transitDisabledSelectsOnlyStreetRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withTransit(tb -> tb.disable()))
      .buildRequest();
    var worker = workerFor(request);

    assertThat(worker.selectRouters()).hasSize(1);
  }

  @Test
  void startOnBoardAccessSkipsAllDirectRoutingAndOnlySelectsTransit() {
    var request = RouteRequest.of().withFrom(ON_BOARD_FROM).withTo(TO).buildRequest();
    var worker = workerFor(request);

    assertThat(worker.selectRouters()).hasSize(1);
  }

  @Test
  void viaSearchSkipsDirectStreetRouterButNotTransit() {
    var via = new VisitViaLocation("Via", null, List.of(), new WgsCoordinate(59.5, 11.0));
    var request = baseRequest().withViaLocations(List.of(via)).buildRequest();
    var worker = workerFor(request);

    // Street router is skipped for via-searches; transit still runs. WALK direct mode does not
    // trigger flex/carpool, so only the transit router remains.
    assertThat(worker.selectRouters()).hasSize(1);
  }

  @Test
  void nonViaSearchWithSameSetupSelectsStreetAndTransitForComparison() {
    var request = baseRequest().buildRequest();
    var worker = workerFor(request);

    assertThat(worker.selectRouters()).hasSize(2);
  }

  @Test
  void notSetDirectModeWithoutPageCursorFallsBackToWalkAndKeepsStreetRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.NOT_SET)))
      .withJourney(jb -> jb.withTransit(tb -> tb.disable()))
      .buildRequest();
    var worker = workerFor(request);

    // No page cursor -> FilterTransitWhenDirectModeIsEmpty resolves NOT_SET to WALK.
    assertThat(worker.selectRouters()).hasSize(1);
  }

  @Test
  void notSetDirectModeWithPageCursorSkipsStreetRouter() {
    var pageCursor = new PageCursor(
      PageType.NEXT_PAGE,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      java.time.Instant.now(),
      null,
      java.time.Duration.ofMinutes(30),
      null,
      null
    );
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.NOT_SET)))
      .withJourney(jb -> jb.withTransit(tb -> tb.disable()))
      .withPageCursorFromEncoded(pageCursor.encode())
      .buildRequest();
    var worker = workerFor(request);

    // With a page cursor present, NOT_SET stays NOT_SET -> street router excluded, and transit
    // is disabled, so nothing is selected at all.
    assertThat(worker.selectRouters()).isEmpty();
  }
}
