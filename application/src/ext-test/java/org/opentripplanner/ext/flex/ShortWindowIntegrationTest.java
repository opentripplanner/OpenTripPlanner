package org.opentripplanner.ext.flex;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetMode.FLEXIBLE;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.core.model.time.LocalDateInterval;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.graph_builder.module.transfer.DirectTransferGenerator;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundleTestFactory;
import org.opentripplanner.gtfs.graphbuilder.GtfsModuleTestFactory;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This test checks the combination of transit and flex works.
 */
public class ShortWindowIntegrationTest {

  private static final GenericLocation FROM = GenericLocation.fromCoordinate(45.39384, -122.2809);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(45.39136, -122.2420);
  private static final Instant TIME = Instant.parse("2026-02-12T20:57:32Z");

  static RoutingService service;

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    TestOtpModel model = FlexIntegrationTestData.sandyOsm();
    addGtfsToGraph(
      model.graph(),
      model.timetableRepository(),
      model.transferRepository(),
      List.of(FlexIntegrationTestData.SANDY_FLEX)
    );
    service = TestServerContext.createServerContext(
      model.graph(),
      model.timetableRepository(),
      model.transferRepository(),
      model.fareServiceFactory().makeFareService()
    ).routingService();
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  @Test
  void shouldReturnARouteTransferringFromBusToFlex() {
    var response = getItineraries(FROM, TO);
    assertThat(response.getRoutingErrors()).isEmpty();
    var itins = response.getTripPlan().itineraries;
    assertThat(itins).hasSize(11);
  }

  private static void addGtfsToGraph(
    Graph graph,
    TimetableRepository timetableRepository,
    TransferRepository transferRepository,
    List<File> gtfsFiles
  ) {
    // GTFS
    var gtfsBundles = gtfsFiles.stream().map(GtfsBundleTestFactory::forTest).toList();
    var gtfsModule = GtfsModuleTestFactory.forTest(
      gtfsBundles,
      timetableRepository,
      graph,
      LocalDateInterval.unbounded()
    );
    gtfsModule.buildGraph();

    // link stations to streets
    TestStreetLinkerModule.link(graph, timetableRepository);

    // link flex locations to streets
    new AreaStopsToVerticesMapper(graph, timetableRepository).buildGraph();

    // we don't have a complete coverage of the entire area so use straight lines for transfers
    new DirectTransferGenerator(
      graph,
      timetableRepository,
      transferRepository,
      DataImportIssueStore.NOOP,
      Duration.ofMinutes(10),
      List.of(RouteRequest.defaultValue())
    ).buildGraph();

    timetableRepository.index();
    graph.index();
    transferRepository.index();
  }

  private static RoutingResponse getItineraries(GenericLocation from, GenericLocation to) {
    RouteRequest request = RouteRequest.of()
      .withDateTime(TIME)
      .withFrom(from)
      .withTo(to)
      .withSearchWindow(Duration.ofHours(4))
      .withPreferences(p ->
        p.withStreet(s ->
          s.withAccessEgress(ae -> ae.withPenalty(Map.of(FLEXIBLE, TimeAndCostPenalty.ZERO)))
        )
      )
      .withJourney(journeyBuilder -> {
        var modes = JourneyRequest.DEFAULT.modes().copyOf();
        modes.withAccessMode(FLEXIBLE).withEgressMode(FLEXIBLE);
        journeyBuilder.withModes(modes.build());
      })
      .buildRequest();

    return service.route(request);
  }
}
