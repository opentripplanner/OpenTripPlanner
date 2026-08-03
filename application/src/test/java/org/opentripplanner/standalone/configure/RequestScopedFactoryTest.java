package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import dagger.BindsInstance;
import dagger.Component;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitDataTestFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transfer.regular.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.regular.internal.TransferIndex;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositoryLifecycle;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;

/**
 * Verifies the real Dagger scoping added for issue #7441: bindings inside one {@link
 * RequestScopedFactory} build (one simulated HTTP request) are cached and shared, while two
 * separate builds (two requests) get independent instances. Since Step 3, {@link
 * OtpServerRequestContext} itself is one of those bindings, so this test's root {@code
 * TestFactory} has to feed every dependency {@link RequestScopedModule#serverRequestContext} needs
 * — reusing the same construction recipes as {@link TestServerContext} rather than duplicating
 * them.
 */
class RequestScopedFactoryTest {

  @Test
  void requestScopedBindingsAreCachedWithinOneRequestButNotAcrossRequests() {
    var transitRepository = new TransitRepository();
    var repositoryRegistry = TransactionFactory.createRepositoryRegistry();
    var timetableSnapshot = new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      new DefaultTripCalendars()
    );
    var timetableRepositoryHandle = repositoryRegistry.registerRepositorySnapshot(
      timetableSnapshot,
      new TimetableRepositoryLifecycle(timetableSnapshot, false, () -> LocalDate.of(2026, 1, 1))
    );

    var routerConfig = RouterConfig.DEFAULT;
    var graph = new Graph();
    var vertexLinker = VertexLinkerTestFactory.of(graph);
    var transferRepository = new DefaultTransferRepository(new TransferIndex());
    // Only used to wire up the throwaway helper services below; not part of what's under test.
    var placeholderTransitService = new DefaultTransitService(transitRepository);

    var factory = DaggerRequestScopedFactoryTest_TestFactory.builder()
      .transitRepository(transitRepository)
      .repositoryRegistry(repositoryRegistry)
      .timetableRepositoryHandle(timetableRepositoryHandle)
      .routerConfig(routerConfig)
      .debugUiConfig(DebugUiConfig.DEFAULT)
      .raptorConfig(
        new RaptorConfig<TripSchedule>(
          routerConfig.transitTuningConfig(),
          RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
        )
      )
      .graph(graph)
      .linkingContextFactory(
        TestServerContext.createLinkingContextFactory(
          graph,
          vertexLinker,
          placeholderTransitService
        )
      )
      .vertexLinker(vertexLinker)
      .transferService(TransferServiceTestFactory.transferService(transferRepository))
      .worldEnvelopeService(TestServerContext.createWorldEnvelopeService())
      .realtimeVehicleRepository(new DefaultRealtimeVehicleRepository())
      .vehicleRentalService(new DefaultVehicleRentalService())
      .vehicleParkingService(TestServerContext.createVehicleParkingService())
      .rideHailingServices(List.of())
      .viaTransferResolver(
        TestServerContext.createViaTransferResolver(graph, placeholderTransitService)
      )
      .carpoolingService(null)
      .dataOverlayParameterBindings(null)
      .stopConsolidationService(null)
      .streetLimitationParametersService(
        TestServerContext.createStreetLimitationParametersService()
      )
      .emissionItineraryDecorator(null)
      .streetDetailsService(TestServerContext.createStreetDetailsService())
      .gtfsSchema(null)
      .transmodelSchema(null)
      .empiricalDelayService(null)
      .sorlandsbanenService(null)
      .launcherRequestDecorator(request -> request)
      .luceneIndex(null)
      .fareServiceFactory(
        new FareServiceFactory() {
          @Override
          public FareService makeFareService() {
            return new DefaultFareService();
          }

          @Override
          public void processGtfs(FareRulesData fareRuleService) {}

          @Override
          public void configure(JsonNode config) {}
        }
      )
      .build();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isSameInstanceAs(requestOne.transitService());
    assertThat(requestOne.transactionScope()).isSameInstanceAs(requestOne.transactionScope());
    assertThat(requestOne.createServerContext()).isSameInstanceAs(requestOne.createServerContext());
    assertThat(requestOne.createServerContext().transitService()).isSameInstanceAs(
      requestOne.transitService()
    );

    var requestTwo = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isNotSameInstanceAs(requestTwo.transitService());
    assertThat(requestOne.createServerContext()).isNotSameInstanceAs(
      requestTwo.createServerContext()
    );
  }

  @Singleton
  @Component(modules = ConstructApplicationModule.class)
  interface TestFactory {
    RequestScopedFactory.Builder requestScopedFactoryBuilder();

    @Component.Builder
    interface Builder {
      @BindsInstance
      Builder transitRepository(TransitRepository transitRepository);

      @BindsInstance
      Builder repositoryRegistry(RepositoryRegistry repositoryRegistry);

      @BindsInstance
      Builder timetableRepositoryHandle(
        RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableRepositoryHandle
      );

      @BindsInstance
      Builder routerConfig(RouterConfig routerConfig);

      @BindsInstance
      Builder debugUiConfig(DebugUiConfig debugUiConfig);

      @BindsInstance
      Builder raptorConfig(RaptorConfig<TripSchedule> raptorConfig);

      @BindsInstance
      Builder graph(Graph graph);

      @BindsInstance
      Builder linkingContextFactory(LinkingContextFactory linkingContextFactory);

      @BindsInstance
      Builder vertexLinker(VertexLinker vertexLinker);

      @BindsInstance
      Builder transferService(RegularTransferService transferService);

      @BindsInstance
      Builder worldEnvelopeService(WorldEnvelopeService worldEnvelopeService);

      @BindsInstance
      Builder realtimeVehicleRepository(RealtimeVehicleRepository realtimeVehicleRepository);

      @BindsInstance
      Builder vehicleRentalService(VehicleRentalService vehicleRentalService);

      @BindsInstance
      Builder vehicleParkingService(VehicleParkingService vehicleParkingService);

      @BindsInstance
      Builder rideHailingServices(List<RideHailingService> rideHailingServices);

      @BindsInstance
      Builder viaTransferResolver(ViaCoordinateTransferFactory viaTransferResolver);

      @BindsInstance
      Builder carpoolingService(@Nullable CarpoolingService carpoolingService);

      @BindsInstance
      Builder dataOverlayParameterBindings(
        @Nullable DataOverlayParameterBindings dataOverlayParameterBindings
      );

      @BindsInstance
      Builder stopConsolidationService(@Nullable StopConsolidationService stopConsolidationService);

      @BindsInstance
      Builder streetLimitationParametersService(
        StreetLimitationParametersService streetLimitationParametersService
      );

      @BindsInstance
      Builder emissionItineraryDecorator(
        @Nullable @EmissionDecorator ItineraryDecorator emissionItineraryDecorator
      );

      @BindsInstance
      Builder streetDetailsService(StreetDetailsService streetDetailsService);

      @BindsInstance
      Builder gtfsSchema(@Nullable @GtfsSchema GraphQLSchema gtfsSchema);

      @BindsInstance
      Builder transmodelSchema(@Nullable @TransmodelSchema GraphQLSchema transmodelSchema);

      @BindsInstance
      Builder empiricalDelayService(@Nullable EmpiricalDelayService empiricalDelayService);

      @BindsInstance
      Builder sorlandsbanenService(@Nullable SorlandsbanenNorwayService sorlandsbanenService);

      @BindsInstance
      Builder launcherRequestDecorator(
        org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator launcherRequestDecorator
      );

      @BindsInstance
      Builder luceneIndex(@Nullable LuceneIndex luceneIndex);

      @BindsInstance
      Builder fareServiceFactory(FareServiceFactory fareServiceFactory);

      TestFactory build();
    }
  }
}
