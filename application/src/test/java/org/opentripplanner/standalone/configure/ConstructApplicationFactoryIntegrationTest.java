package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.standalone.configure.DaggerBindingKey.of;

import graphql.schema.GraphQLSchema;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.configure.StreetDomain;
import org.opentripplanner.framework.transaction.configure.TransitDomain;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.street.StreetRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.configure.StaticTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.warmup.WarmupLauncher;

/**
 * Verifies, by reflecting over every no-arg accessor on {@link ConstructApplicationFactory}, that
 * each exposed service actually has the DI scope its binding is supposed to have. Every accessor's
 * binding (return type plus qualifier annotation, if any) must be classified into exactly one of
 * the three lists below; an unclassified accessor fails the test.
 */
class ConstructApplicationFactoryIntegrationTest {

  /** Correctly scoped as application-wide singletons today. */
  private static final List<DaggerBindingKey> SINGLETONS = List.of(
    of(ConfigModel.class),
    of(RaptorConfig.class),
    of(Graph.class),
    of(TransitRepository.class),
    of(TransferRepository.class),
    of(WorldEnvelopeRepository.class),
    of(WorldEnvelopeService.class),
    of(RepositoryHandle.class),
    of(VehicleRentalRepository.class),
    of(VehicleRentalService.class),
    of(VehicleParkingRepository.class),
    of(VehicleParkingService.class),
    of(UpdateManager.class, TransitDomain.class),
    of(UpdateManager.class, StreetDomain.class),
    of(DataImportIssueSummary.class),
    of(CarpoolingService.class),
    of(CarpoolingRepository.class),
    of(CarpoolTripVertexResolver.class),
    of(EmissionRepository.class),
    of(StreetDetailsRepository.class),
    of(EmpiricalDelayRepository.class),
    of(DelegatingTransitAlertServiceImpl.class),
    of(StopConsolidationRepository.class),
    of(StreetRepository.class),
    of(SorlandsbanenNorwayService.class),
    of(TransitService.class, StaticTransitService.class),
    of(GraphQLSchema.class, GtfsSchema.class),
    of(GraphQLSchema.class, TransmodelSchema.class),
    of(LuceneIndex.class),
    of(FareServiceFactory.class),
    of(DeduplicatorService.class),
    of(WarmupLauncher.class),
    of(MetricsLogging.class)
  );

  /**
   * Meant to be application singletons, but the underlying binding is unscoped, so Dagger builds a
   * fresh instance on every access. Move a binding to {@link #SINGLETONS} once it's fixed.
   */
  private static final List<DaggerBindingKey> KNOWN_UNSCOPED_BUGS = List.of(
    of(LinkingContextFactory.class),
    of(VertexLinker.class),
    of(ViaCoordinateTransferFactory.class)
  );

  /** Dagger subcomponent builders are cheap and mutable — a fresh one every call is by design. */
  private static final List<DaggerBindingKey> PROTOTYPE_BY_DESIGN = List.of(
    of(RequestScopedFactory.Builder.class)
  );

  @Test
  void everyExposedServiceHasTheExpectedScope() throws ReflectiveOperationException {
    var factory = TestConstructApplicationFactoryBuilder.of().build();
    var accessors = List.of(ConstructApplicationFactory.class.getDeclaredMethods())
      .stream()
      .filter(method -> method.getParameterCount() == 0)
      .toList();

    var unclassified = accessors
      .stream()
      .map(DaggerBindingKey::ofAccessor)
      .filter(
        key ->
          !SINGLETONS.contains(key) &&
          !KNOWN_UNSCOPED_BUGS.contains(key) &&
          !PROTOTYPE_BY_DESIGN.contains(key)
      )
      .toList();

    assertWithMessage(
      "Every accessor on %s must be classified into SINGLETONS, KNOWN_UNSCOPED_BUGS or " +
        "PROTOTYPE_BY_DESIGN, but these are not: %s",
      ConstructApplicationFactory.class.getSimpleName(),
      unclassified
    )
      .that(unclassified)
      .isEmpty();

    var failures = new ArrayList<String>();
    for (var method : accessors) {
      var key = DaggerBindingKey.ofAccessor(method);
      var first = method.invoke(factory);
      var second = method.invoke(factory);

      if (SINGLETONS.contains(key) && first != second) {
        failures.add(method.getName() + "() should be an application singleton but was rebuilt");
      } else if (KNOWN_UNSCOPED_BUGS.contains(key) && first == second) {
        failures.add(
          method.getName() +
            "() is listed in KNOWN_UNSCOPED_BUGS but now returns a stable instance — move " +
            key +
            " to SINGLETONS"
        );
      } else if (PROTOTYPE_BY_DESIGN.contains(key) && first == second) {
        failures.add(method.getName() + "() should build a fresh instance every call");
      }
    }

    assertThat(failures).isEmpty();
  }
}
