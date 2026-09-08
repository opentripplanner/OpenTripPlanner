package org.opentripplanner.standalone.configure;

import static org.opentripplanner.standalone.configure.DaggerBindingKey.of;

import graphql.schema.GraphQLSchema;
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
    of(DataImportIssueSummary.class),
    of(DeduplicatorService.class),
    of(DelegatingTransitAlertServiceImpl.class),
    of(FareServiceFactory.class),
    of(Graph.class),
    of(GraphQLSchema.class, GtfsSchema.class),
    of(GraphQLSchema.class, TransmodelSchema.class),
    of(MetricsLogging.class),
    of(RaptorConfig.class),
    of(RepositoryHandle.class),
    of(StreetDetailsRepository.class),
    of(StreetRepository.class),
    of(TransferRepository.class),
    of(TransitRepository.class),
    of(TransitService.class, StaticTransitService.class),
    of(UpdateManager.class, StreetDomain.class),
    of(UpdateManager.class, TransitDomain.class),
    of(VehicleParkingRepository.class),
    of(VehicleParkingService.class),
    of(VehicleRentalRepository.class),
    of(VehicleRentalService.class),
    of(WarmupLauncher.class),
    of(WorldEnvelopeRepository.class),
    of(WorldEnvelopeService.class),
    // Sandbox
    of(CarpoolingRepository.class),
    of(CarpoolingService.class),
    of(CarpoolTripVertexResolver.class),
    of(EmissionRepository.class),
    of(EmpiricalDelayRepository.class),
    of(LuceneIndex.class),
    of(SorlandsbanenNorwayService.class),
    of(StopConsolidationRepository.class)
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
  void everyExposedServiceHasTheExpectedScope() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();
    DaggerScopeAssertions.assertSingleInstanceScope(
      ConstructApplicationFactory.class,
      factory,
      SINGLETONS,
      KNOWN_UNSCOPED_BUGS,
      PROTOTYPE_BY_DESIGN
    );
  }
}
