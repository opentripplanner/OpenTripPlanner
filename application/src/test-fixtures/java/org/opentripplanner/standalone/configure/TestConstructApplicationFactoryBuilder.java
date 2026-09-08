package org.opentripplanner.standalone.configure;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.empiricaldelay.internal.DefaultEmpiricalDelayRepository;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.TransitTestEnvironment;

/**
 * Builds the real production {@link ConstructApplicationFactory} — the same Dagger component
 * {@link ConstructApplication} builds when OTP boots — pre-filled with test defaults, so a test
 * that needs it (e.g. to verify request-vs-application scoping) doesn't have to hand-supply all
 * {@code @BindsInstance} inputs itself.
 * <p>
 * There are no "heavy" resources in OTP (no database, no external service) — every repository is
 * an in-memory object — so every default except the transit repository is a fresh, empty instance.
 * That's normally fine: a test that doesn't care about vehicle parking data, for example, doesn't
 * need real vehicle parking data to exercise the DI wiring around it.
 * <p>
 * Add a {@code withRepositoryX}/{@code withTestEnvironmentX} method to the builder on a need basis,
 * when a test actually needs to override one of the other defaults — don't add one speculatively.
 */
public final class TestConstructApplicationFactoryBuilder {

  private TestConstructApplicationFactoryBuilder() {}

  public static TestConstructApplicationFactoryBuilder of() {
    return new TestConstructApplicationFactoryBuilder();
  }

  public ConstructApplicationFactory build() {
    var transitRepository = TransitTestEnvironment.of().build().transitRepository();

    return DaggerConstructApplicationFactory.builder()
      .configModel(new ConfigModel(OtpConfigLoader.fromString("{}")))
      .graph(new Graph())
      .dataImportIssueSummary(DataImportIssueSummary.empty())
      .stopConsolidationRepository(null)
      .streetDetailsRepository(new DefaultStreetDetailsRepository())
      .transferRepository(TransferServiceTestFactory.defaultTransferRepository())
      .transitRepository(transitRepository)
      .vehicleParkingRepository(new DefaultVehicleParkingRepository())
      .worldEnvelopeRepository(new DefaultWorldEnvelopeRepository())
      .scheduledRaptorTransitData(transitRepository.getRaptorTransitData())
      .scheduledTripCalendars(transitRepository.getTripCalendar())
      .schema(RouteRequest.defaultValue())
      .streetStreetRepository(new DefaultStreetRepository())
      // Sandbox
      .emissionRepository(new DefaultEmissionRepository())
      .empiricalDelayRepository(new DefaultEmpiricalDelayRepository())
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
  }
}
