package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.configure.DaggerBindingKey.of;

import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLRequestContext;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.transaction.TransactionScope;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.configure.DaggerBindingKey.GenericType;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Verifies the Dagger scoping of bindings exposed by {@link RequestScopedFactory}, built through
 * the real production {@link ConstructApplicationFactory}.
 * <p>
 * Every accessor's binding (including qualifier, if any) must be classified into exactly one of the
 * lists below; an unclassified accessor fails the test.
 */
class RequestScopedFactoryIntegrationTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;
  private static final int AWAIT_COMMIT_TIMEOUT_MS = 5_000;
  private static final int AWAIT_COMMIT_POLL_INTERVAL_MS = 20;
  private static final int FROM_STOP = 0;
  private static final int TO_STOP = 1;
  private static final NearbyStop PATH = new NearbyStop(
    FeedScopedId.of("F", "B"),
    100.0,
    List.of(),
    null
  );

  /**
   * Application-wide singletons, just exposed through the request scope for convenience: the same
   * instance within one request, and the same instance across two independent requests.
   */
  private static final List<DaggerBindingKey> APPLICATION_SINGLETON = List.of(
    of(DebugUiConfig.class),
    of(FareService.class),
    of(Graph.class),
    // Unqualified: GtfsGraphQLRequestContext#schema() needs no qualifier to disambiguate, since
    // the context is already GTFS-specific, but it's the exact same binding as GraphQLSchema
    // below.
    of(GraphQLSchema.class, GtfsSchema.class),
    of(GraphQLSchema.class, TransmodelSchema.class),
    of(GtfsApiParameters.class),
    of(RegularTransferService.class),
    // Without a commit in between, both requests read the same snapshot
    of(new GenericType<RegularTransferRepositorySnapshot<NearbyStop, RouteRequest>>() {}),
    of(RouteRequest.class),
    of(TransitAlertService.class),
    of(TransmodelAPIParameters.class),
    of(VectorTileConfig.class),
    of(VehicleParkingService.class),
    of(VehicleRentalService.class),
    of(WorldEnvelopeService.class),
    // Sandbox
    // Nullable and off by default in this test's config, so both requests observe null.
    of(EmpiricalDelayService.class),
    of(LuceneIndex.class),
    of(OjpApiParameters.class),
    of(TriasApiParameters.class)
  );

  /**
   * Correctly {@code @HttpRequestScoped}: same instance within one request build, a different
   * instance across two independent request builds.
   */
  private static final List<DaggerBindingKey> REQUEST_SCOPED = List.of(
    of(GtfsGraphQLRequestContext.class),
    of(RealtimeVehicleService.class),
    of(RoutingService.class),
    of(TransactionScope.class),
    of(TransitService.class),
    of(TransmodelGraphQLRequestContext.class)
  );

  /**
   * Genuinely unscoped: a fresh instance is built even for two accesses within the same request
   * build. Move a binding to {@link #APPLICATION_SINGLETON} or {@link #REQUEST_SCOPED} once it's
   * fixed.
   */
  private static final List<DaggerBindingKey> KNOWN_UNSCOPED_BUGS = List.of(
    of(LinkingContextFactory.class),
    of(StreetDetailsService.class),
    of(StreetLimitationParametersService.class)
  );

  /** Nothing exposed directly by {@link RequestScopedFactory} is a hand-memoized, non-Dagger value. */
  /**
   * Guards against the #7441 regression: a binding reached through a nested consumer ({@link
   * GtfsGraphQLRequestContext}) must resolve to the same shared instance as the direct accessor
   * on {@link RequestScopedFactory}, which is all the scope check below covers.
   */
  @Test
  void nestedConsumersShareTheSameInstancesAsDirectAccessors() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.gtfsRequestContext().transitService()).isSameInstanceAs(
      requestOne.transitService()
    );
    assertThat(requestOne.gtfsRequestContext().schema()).isSameInstanceAs(requestOne.gtfsSchema());

    var requestTwo = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.gtfsRequestContext().schema()).isSameInstanceAs(
      requestTwo.gtfsRequestContext().schema()
    );
  }

  @Test
  void everyExposedServiceHasTheExpectedScope() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();
    var requestOne = factory.requestScopedFactoryBuilder().build();
    var requestTwo = factory.requestScopedFactoryBuilder().build();
    DaggerScopeAssertions.requestScope(RequestScopedFactory.class, requestOne, requestTwo)
      .applicationSingleton(APPLICATION_SINGLETON)
      .requestScoped(REQUEST_SCOPED)
      .knownUnscopedBugs(KNOWN_UNSCOPED_BUGS)
      .assertScopes();
  }

  /**
   * The production wiring turns the build repository into the initial repository snapshot, which
   * requests read through their scope, and seals the build repository.
   */
  @Test
  void requestsReadTheTransfersOfTheBuildRepository() {
    RegularTransferBuildRepository<NearbyStop> buildRepository =
      RegularTransferFactory.createBuildRepository();
    buildRepository.setPaths(WALK, FROM_STOP, Map.of(TO_STOP, PATH));
    var factory = TestConstructApplicationFactoryBuilder.of()
      .withRegularTransferBuildRepository(buildRepository)
      .build();

    var request = factory.requestScopedFactoryBuilder().build();

    assertThat(findPath(request)).isSameInstanceAs(PATH);
    assertThrows(IllegalStateException.class, () ->
      buildRepository.setPaths(WALK, TO_STOP, Map.of(FROM_STOP, PATH))
    );
  }

  /**
   * A request reads the regular transfers of the snapshot it started with, also after an update
   * is committed. Requests started after the commit see the update.
   */
  @Test
  void requestsReadTheRegularTransfersOfTheirSnapshot() throws Exception {
    // A path in the opposite direction, so the profile exists
    RegularTransferBuildRepository<NearbyStop> buildRepository =
      RegularTransferFactory.createBuildRepository();
    buildRepository.setPaths(WALK, TO_STOP, Map.of(FROM_STOP, PATH));
    var factory = TestConstructApplicationFactoryBuilder.of()
      .withRegularTransferBuildRepository(buildRepository)
      .build();
    var handle = factory.regularTransferRepositoryHandle();
    var updateManager = factory.transitUpdateManager();
    try {
      var before = factory.requestScopedFactoryBuilder().build();
      assertThat(findPath(before)).isNull();

      updateManager
        .submit(ctx ->
          ctx.repository(handle).replaceTransfersFrom(WALK, FROM_STOP, Map.of(TO_STOP, PATH))
        )
        .get();
      var after = awaitRequestSeeing(factory, PATH);

      assertThat(findPath(after)).isSameInstanceAs(PATH);
      assertThat(findPath(before)).isNull();
    } finally {
      updateManager.shutdown();
    }
  }

  @Nullable
  private static NearbyStop findPath(RequestScopedFactory requestFactory) {
    return requestFactory
      .regularTransfers()
      .forRequest(RouteRequest.defaultValue())
      .findPath(FROM_STOP, TO_STOP);
  }

  /** The transit domain commits periodically, so poll until a new request sees the update. */
  private static RequestScopedFactory awaitRequestSeeing(
    ConstructApplicationFactory factory,
    NearbyStop expected
  ) throws InterruptedException {
    long timeout = System.currentTimeMillis() + AWAIT_COMMIT_TIMEOUT_MS;
    while (true) {
      var request = factory.requestScopedFactoryBuilder().build();
      if (findPath(request) == expected || System.currentTimeMillis() > timeout) {
        return request;
      }
      Thread.sleep(AWAIT_COMMIT_POLL_INTERVAL_MS);
    }
  }
}
