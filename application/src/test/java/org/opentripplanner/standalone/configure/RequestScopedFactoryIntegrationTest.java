package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.standalone.configure.DaggerBindingKey.of;

import graphql.schema.GraphQLSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLRequestContext;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
import org.opentripplanner.framework.transaction.api.TransactionScope;
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
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Verifies the Dagger scoping of bindings exposed by {@link RequestScopedFactory}, built through
 * the real production {@link ConstructApplicationFactory}.
 * <p>
 * Every accessor's binding (including qualifier, if any) must be classified into exactly one of the
 * lists below; an unclassified accessor fails the test.
 */
class RequestScopedFactoryIntegrationTest {

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
  private static final List<DaggerBindingKey> IGNORED = List.of();

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
    DaggerScopeAssertions.assertRequestScope(
      RequestScopedFactory.class,
      requestOne,
      requestTwo,
      APPLICATION_SINGLETON,
      REQUEST_SCOPED,
      KNOWN_UNSCOPED_BUGS,
      IGNORED
    );
  }
}
