package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.standalone.configure.DaggerBindingKey.of;

import graphql.schema.GraphQLSchema;
import java.util.ArrayList;
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
 * Verifies the real Dagger scoping added for bindings inside the {@link RequestScopedFactory}.
 * <p>
 * This test builds the real production {@link ConstructApplicationFactory}. That way every real
 * module (including the ones outside {@link RequestScopedModule} itself) is actually exercised
 * through Dagger, not substituted with a hand-built instance.
 * <p>
 * This test reflects over every no-arg accessor on {@link RequestScopedFactory} and checks it has
 * the DI scope its binding is supposed to have. Every accessor's binding (return type plus
 * qualifier annotation, if any) must be classified into exactly one of the three lists below; an
 * unclassified accessor fails the test.
 */
class RequestScopedFactoryIntegrationTest {

  /**
   * Application-wide singletons, just exposed through the request scope for convenience: same
   * instance within one request, and the same instance across two independent requests.
   */
  private static final List<DaggerBindingKey> APPLICATION_SINGLETON = List.of(
    of(Graph.class),
    of(GraphQLSchema.class, GtfsSchema.class),
    of(GraphQLSchema.class, TransmodelSchema.class),
    of(VehicleRentalService.class),
    of(VehicleParkingService.class),
    of(TransitAlertService.class),
    of(RouteRequest.class),
    of(DebugUiConfig.class),
    of(FareService.class),
    of(RegularTransferService.class),
    of(LuceneIndex.class),
    of(VectorTileConfig.class),
    of(GtfsApiParameters.class),
    of(TransmodelAPIParameters.class),
    of(OjpApiParameters.class),
    of(TriasApiParameters.class),
    of(WorldEnvelopeService.class),
    // Nullable and off by default in this test's config, so both requests observe null.
    of(EmpiricalDelayService.class)
  );

  /**
   * Correctly {@code @HttpRequestScoped}: same instance within one request build, a different
   * instance across two independent request builds.
   */
  private static final List<DaggerBindingKey> REQUEST_SCOPED = List.of(
    of(TransactionScope.class),
    of(TransitService.class),
    of(RoutingService.class),
    of(RealtimeVehicleService.class),
    of(GtfsGraphQLRequestContext.class),
    of(TransmodelGraphQLRequestContext.class)
  );

  /**
   * Genuinely unscoped: a fresh instance is built even for two accesses within the same request
   * build. Move a binding to {@link #APPLICATION_SINGLETON} or {@link #REQUEST_SCOPED} once it's
   * fixed.
   */
  private static final List<DaggerBindingKey> KNOWN_UNSCOPED_BUGS = List.of(
    of(StreetDetailsService.class),
    of(LinkingContextFactory.class),
    of(StreetLimitationParametersService.class)
  );

  @Test
  void requestScopedBindingsAreCachedWithinOneRequestButNotAcrossRequests() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isSameInstanceAs(requestOne.transitService());
    assertThat(requestOne.transactionScope()).isSameInstanceAs(requestOne.transactionScope());
    assertThat(requestOne.gtfsRequestContext()).isSameInstanceAs(requestOne.gtfsRequestContext());
    assertThat(requestOne.gtfsRequestContext().transitService()).isSameInstanceAs(
      requestOne.transitService()
    );

    var requestTwo = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isNotSameInstanceAs(requestTwo.transitService());
    assertThat(requestOne.gtfsRequestContext()).isNotSameInstanceAs(
      requestTwo.gtfsRequestContext()
    );
    assertThat(requestOne.gtfsRequestContext().schema()).isSameInstanceAs(
      requestTwo.gtfsRequestContext().schema()
    );
    assertThat(requestOne.transmodelGraphQLSchema()).isSameInstanceAs(
      requestTwo.transmodelGraphQLSchema()
    );
  }

  @Test
  void everyExposedServiceHasTheExpectedScope() throws ReflectiveOperationException {
    var factory = TestConstructApplicationFactoryBuilder.of().build();
    var accessors = List.of(RequestScopedFactory.class.getDeclaredMethods())
      .stream()
      .filter(method -> method.getParameterCount() == 0)
      .toList();

    var unclassified = accessors
      .stream()
      .map(DaggerBindingKey::ofAccessor)
      .filter(
        key ->
          !APPLICATION_SINGLETON.contains(key) &&
          !REQUEST_SCOPED.contains(key) &&
          !KNOWN_UNSCOPED_BUGS.contains(key)
      )
      .toList();

    assertWithMessage(
      "Every accessor on %s must be classified into APPLICATION_SINGLETON, REQUEST_SCOPED or " +
        "KNOWN_UNSCOPED_BUGS, but these are not: %s",
      RequestScopedFactory.class.getSimpleName(),
      unclassified
    )
      .that(unclassified)
      .isEmpty();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    var requestTwo = factory.requestScopedFactoryBuilder().build();
    var failures = new ArrayList<String>();

    for (var method : accessors) {
      var key = DaggerBindingKey.ofAccessor(method);
      var withinRequestFirst = method.invoke(requestOne);
      var withinRequestSecond = method.invoke(requestOne);
      var acrossRequests = method.invoke(requestTwo);

      if (KNOWN_UNSCOPED_BUGS.contains(key)) {
        if (withinRequestFirst == withinRequestSecond) {
          failures.add(
            method.getName() +
              "() is listed in KNOWN_UNSCOPED_BUGS but is now stable within one request — " +
              "reclassify " +
              key
          );
        }
        continue;
      }

      if (withinRequestFirst != withinRequestSecond) {
        failures.add(method.getName() + "() should be cached within one request but was rebuilt");
        continue;
      }

      if (APPLICATION_SINGLETON.contains(key) && withinRequestFirst != acrossRequests) {
        failures.add(
          method.getName() + "() should be stable across requests but differed across requests"
        );
      } else if (REQUEST_SCOPED.contains(key) && withinRequestFirst == acrossRequests) {
        failures.add(
          method.getName() +
            "() is listed as REQUEST_SCOPED but returned the same instance across two " +
            "independent requests — move " +
            key +
            " to APPLICATION_SINGLETON"
        );
      }
    }

    assertThat(failures).isEmpty();
  }
}
