package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This test checks the combination of transit and flex works.
 */
public class FlexIntegrationTest {

  public static final GenericLocation OUTSIDE_FLEX_ZONE = new GenericLocation(33.7552, -84.4631);
  public static final GenericLocation INSIDE_FLEX_ZONE = new GenericLocation(33.8694, -84.6233);
  static Instant dateTime = ZonedDateTime.parse(
    "2021-12-02T12:00:00-05:00[America/New_York]"
  ).toInstant();

  static Graph graph;

  static TimetableRepository timetableRepository;

  static RoutingService service;

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    TestOtpModel model = FlexIntegrationTestData.cobbOsm();
    graph = model.graph();
    timetableRepository = model.timetableRepository();
    addGtfsToGraph(
      graph,
      timetableRepository,
      List.of(
        FlexIntegrationTestData.COBB_BUS_30_GTFS,
        FlexIntegrationTestData.MARTA_BUS_856_GTFS,
        FlexIntegrationTestData.COBB_FLEX_GTFS
      )
    );
    service = TestServerContext.createServerContext(
      graph,
      timetableRepository,
      model.fareServiceFactory().makeFareService()
    ).routingService();
  }

  @Test
  void addFlexTripsAndPatternsToGraph() {
    assertFalse(timetableRepository.getAllTripPatterns().isEmpty());
  }

  @Test
  void shouldReturnARouteTransferringFromBusToFlex() {
    var itin = getItinerary(OUTSIDE_FLEX_ZONE, INSIDE_FLEX_ZONE, 4);

    assertEquals(4, itin.legs().size());

    var walkToBus = itin.streetLeg(0);
    assertEquals(WALK, walkToBus.getMode());

    var bus = itin.transitLeg(1);
    assertEquals(BUS, bus.mode());
    assertEquals("30", bus.route().getShortName());

    var transfer = itin.streetLeg(2);
    assertEquals(WALK, transfer.getMode());

    var flex = itin.transitLeg(3);
    assertEquals(BUS, flex.mode());
    assertEquals("Zone 2", flex.route().getShortName());
    assertTrue(flex.isFlexibleTrip());
    assertEquals(
      "corner of Story Place Southwest and service road (part of Flex Zone 2)",
      flex.from().name.toString()
    );
    assertEquals("Destination (part of Flex Zone 2)", flex.to().name.toString());
    assertEquals("2021-12-02T14:30-05:00[America/New_York]", flex.startTime().toString());
    assertEquals("2021-12-02T15:00-05:00[America/New_York]", flex.endTime().toString());
  }

  @Test
  void shouldReturnARouteWithTwoTransfers() {
    var from = GenericLocation.fromStopId("ALEX DR@ALEX WAY", "MARTA", "97266");
    var to = new GenericLocation(33.86701256815635, -84.61787939071655);

    var itin = getItinerary(from, to, 3);

    assertEquals(5, itin.legs().size());

    var firstBus = itin.transitLeg(0);
    assertEquals(BUS, firstBus.mode());
    assertEquals("856", firstBus.route().getShortName());

    var transferToSecondBus = itin.streetLeg(1);
    assertEquals(WALK, transferToSecondBus.getMode());

    var secondBus = itin.transitLeg(2);
    assertEquals(BUS, secondBus.mode());
    assertEquals("30", secondBus.route().getShortName());

    var transferToFlex = itin.streetLeg(3);
    assertEquals(WALK, transferToFlex.getMode());

    var finalFlex = itin.transitLeg(4);
    assertEquals(BUS, finalFlex.mode());
    assertEquals("Zone 2", finalFlex.route().getShortName());
    assertTrue(finalFlex.isFlexibleTrip());
    assertEquals("2021-12-02T15:00-05:00[America/New_York]", finalFlex.startTime().toString());
    assertEquals("2021-12-02T15:30-05:00[America/New_York]", finalFlex.endTime().toString());
  }

  @Test
  void flexDirect() {
    // near flex zone
    var from = new GenericLocation(33.85281, -84.60271);
    // in the middle of flex zone
    var to = new GenericLocation(33.86701256815635, -84.61787939071655);

    List<Itinerary> itineraries = getItineraries(from, to, true);

    assertTrue(
      itineraries.stream().noneMatch(Itinerary::isWalkOnly),
      "Should contain only flex itineraries"
    );

    var itin = itineraries.get(0);

    // walk, flex
    assertEquals(2, itin.legs().size());
    assertEquals("2021-12-02T12:52:54-05:00[America/New_York]", itin.startTime().toString());
    assertEquals(3203, itin.generalizedCost());

    var walkToFlex = itin.streetLeg(0);
    assertEquals(WALK, walkToFlex.getMode());

    var flex = itin.transitLeg(1);
    assertEquals(BUS, flex.mode());
    assertEquals("Zone 2", flex.route().getShortName());
    assertTrue(flex.isFlexibleTrip());

    assertEquals("Transfer Point for Route 30", flex.from().name.toString());
    assertEquals("Destination (part of Flex Zone 2)", flex.to().name.toString());

    assertEquals("2021-12-02T13:00-05:00[America/New_York]", flex.startTime().toString());
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  private static void addGtfsToGraph(
    Graph graph,
    TimetableRepository timetableRepository,
    List<File> gtfsFiles
  ) {
    // GTFS
    var gtfsBundles = gtfsFiles.stream().map(GtfsBundle::forTest).toList();
    GtfsModule gtfsModule = GtfsModule.forTest(
      gtfsBundles,
      timetableRepository,
      graph,
      ServiceDateInterval.unbounded()
    );
    gtfsModule.buildGraph();

    // link stations to streets
    TestStreetLinkerModule.link(graph, timetableRepository);

    // link flex locations to streets
    new AreaStopsToVerticesMapper(graph, timetableRepository).buildGraph();

    // generate direct transfers
    var req = new RouteRequest();

    // we don't have a complete coverage of the entire area so use straight lines for transfers
    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      Duration.ofMinutes(10),
      List.of(req)
    ).buildGraph();

    timetableRepository.index();
    graph.index();
  }

  private Itinerary getItinerary(GenericLocation from, GenericLocation to, int index) {
    List<Itinerary> itineraries = getItineraries(from, to, false);
    return itineraries.get(index);
  }

  private static List<Itinerary> getItineraries(
    GenericLocation from,
    GenericLocation to,
    boolean onlyDirect
  ) {
    RouteRequest request = new RouteRequest();
    request.setDateTime(dateTime);
    request.setFrom(from);
    request.setTo(to);
    request.setNumItineraries(10);
    request.setSearchWindow(Duration.ofHours(2));
    request.withPreferences(p ->
      p.withStreet(s ->
        s.withAccessEgress(ae -> ae.withPenalty(Map.of(FLEXIBLE, TimeAndCostPenalty.ZERO)))
      )
    );

    var modes = request.journey().modes().copyOf();

    if (onlyDirect) {
      modes
        .withDirectMode(FLEXIBLE)
        .withAccessMode(StreetMode.WALK)
        .withEgressMode(StreetMode.WALK);
      request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
      request.journey().transit().disable();
    } else {
      modes.withEgressMode(FLEXIBLE);
      request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    }

    request.journey().setModes(modes.build());

    var result = service.route(request);
    var itineraries = result.getTripPlan().itineraries;

    assertFalse(itineraries.isEmpty());
    return itineraries;
  }
}
