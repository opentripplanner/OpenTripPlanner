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
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
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
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

/**
 * This test checks the combination of transit and flex works.
 */
public class FlexIntegrationTest {

  public static final GenericLocation OUTSIDE_FLEX_ZONE = new GenericLocation(33.7552, -84.4631);
  public static final GenericLocation INSIDE_FLEX_ZONE = new GenericLocation(33.8694, -84.6233);
  static Instant dateTime = ZonedDateTime
    .parse("2021-12-02T12:00:00-05:00[America/New_York]")
    .toInstant();

  static Graph graph;

  static TransitModel transitModel;

  static RoutingService service;

  @BeforeAll
  static void setup() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    TestOtpModel model = ConstantsForTests.buildOsmGraph(FlexTest.COBB_OSM);
    graph = model.graph();
    transitModel = model.transitModel();

    addGtfsToGraph(
      graph,
      transitModel,
      List.of(FlexTest.COBB_BUS_30_GTFS, FlexTest.MARTA_BUS_856_GTFS, FlexTest.COBB_FLEX_GTFS)
    );
    service = TestServerContext.createServerContext(graph, transitModel).routingService();
  }

  @Test
  void addFlexTripsAndPatternsToGraph() {
    assertFalse(transitModel.getAllTripPatterns().isEmpty());
  }

  @Test
  void shouldReturnARouteTransferringFromBusToFlex() {
    var itin = getItinerary(OUTSIDE_FLEX_ZONE, INSIDE_FLEX_ZONE, 4);

    assertEquals(4, itin.getLegs().size());

    var walkToBus = itin.getStreetLeg(0);
    assertEquals(WALK, walkToBus.getMode());

    var bus = itin.getTransitLeg(1);
    assertEquals(BUS, bus.getMode());
    assertEquals("30", bus.getRoute().getShortName());

    var transfer = itin.getStreetLeg(2);
    assertEquals(WALK, transfer.getMode());

    var flex = itin.getTransitLeg(3);
    assertEquals(BUS, flex.getMode());
    assertEquals("Zone 2", flex.getRoute().getShortName());
    assertTrue(flex.isFlexibleTrip());
    assertEquals(
      "corner of Story Place Southwest and service road (part of Flex Zone 2)",
      flex.getFrom().name.toString()
    );
    assertEquals("Destination (part of Flex Zone 2)", flex.getTo().name.toString());
    assertEquals("2021-12-02T14:30-05:00[America/New_York]", flex.getStartTime().toString());
    assertEquals("2021-12-02T15:00-05:00[America/New_York]", flex.getEndTime().toString());
  }

  @Test
  void shouldReturnARouteWithTwoTransfers() {
    var from = GenericLocation.fromStopId("ALEX DR@ALEX WAY", "MARTA", "97266");
    var to = new GenericLocation(33.86701256815635, -84.61787939071655);

    var itin = getItinerary(from, to, 3);

    assertEquals(5, itin.getLegs().size());

    var firstBus = itin.getTransitLeg(0);
    assertEquals(BUS, firstBus.getMode());
    assertEquals("856", firstBus.getRoute().getShortName());

    var transferToSecondBus = itin.getStreetLeg(1);
    assertEquals(WALK, transferToSecondBus.getMode());

    var secondBus = itin.getTransitLeg(2);
    assertEquals(BUS, secondBus.getMode());
    assertEquals("30", secondBus.getRoute().getShortName());

    var transferToFlex = itin.getStreetLeg(3);
    assertEquals(WALK, transferToFlex.getMode());

    var finalFlex = itin.getTransitLeg(4);
    assertEquals(BUS, finalFlex.getMode());
    assertEquals("Zone 2", finalFlex.getRoute().getShortName());
    assertTrue(finalFlex.isFlexibleTrip());
    assertEquals("2021-12-02T15:00-05:00[America/New_York]", finalFlex.getStartTime().toString());
    assertEquals("2021-12-02T15:30-05:00[America/New_York]", finalFlex.getEndTime().toString());
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
    assertEquals(2, itin.getLegs().size());
    assertEquals("2021-12-02T12:52:42-05:00[America/New_York]", itin.startTime().toString());
    assertEquals(3203, itin.getGeneralizedCost());

    var walkToFlex = itin.getStreetLeg(0);
    assertEquals(WALK, walkToFlex.getMode());

    var flex = itin.getTransitLeg(1);
    assertEquals(BUS, flex.getMode());
    assertEquals("Zone 2", flex.getRoute().getShortName());
    assertTrue(flex.isFlexibleTrip());

    assertEquals("Transfer Point for Route 30", flex.getFrom().name.toString());
    assertEquals("Destination (part of Flex Zone 2)", flex.getTo().name.toString());

    assertEquals("2021-12-02T13:00-05:00[America/New_York]", flex.getStartTime().toString());
  }

  @AfterAll
  static void teardown() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  private static void addGtfsToGraph(Graph graph, TransitModel transitModel, List<File> gtfsFiles) {
    // GTFS
    var gtfsBundles = gtfsFiles.stream().map(GtfsBundle::new).toList();
    GtfsModule gtfsModule = new GtfsModule(
      gtfsBundles,
      transitModel,
      graph,
      ServiceDateInterval.unbounded()
    );
    gtfsModule.buildGraph();

    // link stations to streets
    TestStreetLinkerModule.link(graph, transitModel);

    // link flex locations to streets
    new AreaStopsToVerticesMapper(graph, transitModel).buildGraph();

    // generate direct transfers
    var req = new RouteRequest();

    // we don't have a complete coverage of the entire area so use straight lines for transfers
    new DirectTransferGenerator(
      graph,
      transitModel,
      DataImportIssueStore.NOOP,
      Duration.ofMinutes(10),
      List.of(req)
    )
      .buildGraph();

    transitModel.index();
    graph.index(transitModel.getStopModel());
  }

  private Itinerary getItinerary(GenericLocation from, GenericLocation to, int index) {
    List<Itinerary> itineraries = getItineraries(from, to, false);
    return itineraries.get(index);
  }

  @Nonnull
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
    modes.withEgressMode(FLEXIBLE);

    if (onlyDirect) {
      modes.withDirectMode(FLEXIBLE);
      request.journey().transit().setFilters(List.of(ExcludeAllTransitFilter.of()));
    } else {
      request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    }

    request.journey().setModes(modes.build());

    var result = service.route(request);
    var itineraries = result.getTripPlan().itineraries;

    assertFalse(itineraries.isEmpty());
    return itineraries;
  }
}
