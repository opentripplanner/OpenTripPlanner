package org.opentripplanner.routing.algorithm;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

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
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.CarpoolAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.DefaultAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.RideHailingAccessEgressRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Unit tests for {@link RouterSelector#selectRouters} and
 * {@link RouterSelector#selectAccessEgressRouters(StreetMode)}, the two places where all
 * mode-equality and feature-flag checks for router selection live.
 */
class RouterSelectorTest {

  private static final GenericLocation FROM = GenericLocation.fromCoordinate(60.0, 10.0);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(59.0, 12.0);
  private static final GenericLocation ON_BOARD_FROM = GenericLocation.fromTripLocation(
    TripLocation.of(
      TripOnDateReference.ofTripOnServiceDateId(new FeedScopedId("F", "trip-1")),
      new FeedScopedId("F", "stop-1")
    )
  );

  /** No-op runner for the direct-street router - never actually invoked by these tests. */
  private static final java.util.function.BiFunction<
    RouteRequest,
    Boolean,
    RoutingResult
  > NOOP_STREET_RUNNER = (req, flag) -> RoutingResult.empty();

  /** No-op runner for the direct-flex/direct-carpool/transit routers. */
  private static final java.util.function.Supplier<RoutingResult> NOOP_RUNNER =
    RoutingResult::empty;

  private static RouteRequestBuilder baseRequest() {
    return RouteRequest.of().withFrom(FROM).withTo(TO);
  }

  private static RouterSelector selectorFor(RouteRequest request) {
    return new RouterSelector(
      request,
      mock(TransitService.class),
      mock(Graph.class),
      mock(RegularTransferService.class),
      mock(StreetDetailsService.class),
      FlexParameters.defaultValues(),
      AdditionalSearchDays.defaults(ZonedDateTime.now(ZoneOffset.UTC)),
      List.<RideHailingService>of(),
      mock(CarpoolingService.class),
      ZonedDateTime.now(ZoneOffset.UTC)
    );
  }

  private static List<java.util.function.Supplier<RoutingResult>> selectRouters(
    RouterSelector selector
  ) {
    return selector.selectRouters(NOOP_STREET_RUNNER, NOOP_RUNNER, NOOP_RUNNER, NOOP_RUNNER);
  }

  // --- selectAccessEgressRouters(StreetMode) ---

  @Test
  void walkAccessEgressUsesOnlyDefaultRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    var routers = selector.selectAccessEgressRouters(StreetMode.WALK);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void flexibleAccessEgressWithFlexRoutingOffUsesOnlyDefaultRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    var routers = selector.selectAccessEgressRouters(StreetMode.FLEXIBLE);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void flexibleAccessEgressWithFlexRoutingOnAddsFlexRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    OTPFeature.FlexRouting.testOn(() -> {
      var routers = selector.selectAccessEgressRouters(StreetMode.FLEXIBLE);

      assertThat(routers).hasSize(2);
      assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
      assertThat(routers.get(1)).isInstanceOf(FlexAccessEgressRouter.class);
    });
  }

  @Test
  void carpoolAccessEgressWithCarPoolingOffUsesOnlyDefaultRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    var routers = selector.selectAccessEgressRouters(StreetMode.CARPOOL);

    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
  }

  @Test
  void carpoolAccessEgressWithCarPoolingOnAddsCarpoolRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    OTPFeature.CarPooling.testOn(() -> {
      var routers = selector.selectAccessEgressRouters(StreetMode.CARPOOL);

      assertThat(routers).hasSize(2);
      assertThat(routers.get(0)).isInstanceOf(DefaultAccessEgressRouter.class);
      assertThat(routers.get(1)).isInstanceOf(CarpoolAccessEgressRouter.class);
    });
  }

  @Test
  void carHailingAccessEgressWrapsDefaultRouter() {
    var selector = selectorFor(baseRequest().buildRequest());

    var routers = selector.selectAccessEgressRouters(StreetMode.CAR_HAILING);

    // CAR_HAILING decorates (time-shifts) the same default street search, rather than adding an
    // independent extra router the way Flex/Carpool do - so there is still only one entry.
    assertThat(routers).hasSize(1);
    assertThat(routers.get(0)).isInstanceOf(RideHailingAccessEgressRouter.class);
  }

  // --- selectRouters(...) ---

  @Test
  void defaultRequestSelectsStreetAndTransit() {
    var selector = selectorFor(baseRequest().buildRequest());

    assertThat(selectRouters(selector)).hasSize(2);
  }

  @Test
  void flexDirectModeWithFlexRoutingOffSkipsFlexRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.FLEXIBLE)))
      .buildRequest();
    var selector = selectorFor(request);

    assertThat(selectRouters(selector)).hasSize(2);
  }

  @Test
  void flexDirectModeWithFlexRoutingOnAddsFlexRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.FLEXIBLE)))
      .buildRequest();
    var selector = selectorFor(request);

    OTPFeature.FlexRouting.testOn(() -> assertThat(selectRouters(selector)).hasSize(3));
  }

  @Test
  void carpoolDirectModeWithCarPoolingOnAddsCarpoolRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.CARPOOL)))
      .buildRequest();
    var selector = selectorFor(request);

    OTPFeature.CarPooling.testOn(() -> assertThat(selectRouters(selector)).hasSize(3));
  }

  @Test
  void transitDisabledSelectsOnlyStreetRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withTransit(tb -> tb.disable()))
      .buildRequest();
    var selector = selectorFor(request);

    assertThat(selectRouters(selector)).hasSize(1);
  }

  @Test
  void startOnBoardAccessSkipsAllDirectRoutingAndOnlySelectsTransit() {
    var request = RouteRequest.of().withFrom(ON_BOARD_FROM).withTo(TO).buildRequest();
    var selector = selectorFor(request);

    assertThat(selectRouters(selector)).hasSize(1);
  }

  @Test
  void viaSearchSkipsDirectStreetRouterButNotTransit() {
    var via = new VisitViaLocation("Via", null, List.of(), new WgsCoordinate(59.5, 11.0));
    var request = baseRequest().withViaLocations(List.of(via)).buildRequest();
    var selector = selectorFor(request);

    // Street router is skipped for via-searches; transit still runs. WALK direct mode does not
    // trigger flex/carpool, so only the transit router remains.
    assertThat(selectRouters(selector)).hasSize(1);
  }

  @Test
  void nonViaSearchWithSameSetupSelectsStreetAndTransitForComparison() {
    var request = baseRequest().buildRequest();
    var selector = selectorFor(request);

    assertThat(selectRouters(selector)).hasSize(2);
  }

  @Test
  void notSetDirectModeWithoutPageCursorFallsBackToWalkAndKeepsStreetRouter() {
    var request = baseRequest()
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.NOT_SET)))
      .withJourney(jb -> jb.withTransit(tb -> tb.disable()))
      .buildRequest();
    var selector = selectorFor(request);

    // No page cursor -> FilterTransitWhenDirectModeIsEmpty resolves NOT_SET to WALK.
    assertThat(selectRouters(selector)).hasSize(1);
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
    var selector = selectorFor(request);

    // With a page cursor present, NOT_SET stays NOT_SET -> street router excluded, and transit
    // is disabled, so nothing is selected at all.
    assertThat(selectRouters(selector)).isEmpty();
  }
}
