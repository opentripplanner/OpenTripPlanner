package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.FakeGraph.getFileForResource;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.core.TraverseMode.BUS;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OTPFeature;

/**
 * This test checks the combination of transit and flex works.
 */
public class FlexIntegrationTest {

    static Instant dateTime = ZonedDateTime.parse("2021-12-02T12:00:00-05:00[America/New_York]")
            .toInstant();

    static Graph graph;
    static RoutingService service;
    static Router router;

    @BeforeAll
    static void setup() {
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
        var osmPath = getAbsolutePath(FlexTest.COBB_OSM);
        var cobblincGtfsPath = getAbsolutePath(FlexTest.COBB_BUS_30_GTFS);
        var martaGtfsPath = getAbsolutePath(FlexTest.MARTA_BUS_856_GTFS);
        var flexGtfsPath = getAbsolutePath(FlexTest.COBB_FLEX_GTFS);

        graph = ConstantsForTests.buildOsmGraph(osmPath);
        addGtfsToGraph(graph, List.of(cobblincGtfsPath, martaGtfsPath, flexGtfsPath));
        router = new Router(graph, RouterConfig.DEFAULT);
        router.startup();

        service = new RoutingService(graph);
    }

    private static String getAbsolutePath(String cobbOsm) {
        try {
            return getFileForResource(cobbOsm).getAbsolutePath();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldReturnARouteTransferringFromBusToFlex() {
        var from = new GenericLocation(33.84329482265106, -84.583740234375);
        var to = new GenericLocation(33.86701256815635, -84.61787939071655);

        var itin = getItinerary(from, to, 2);

        assertEquals(4, itin.legs.size());

        var walkToBus = itin.legs.get(0);
        assertEquals(TraverseMode.WALK, walkToBus.getMode());

        var bus = itin.legs.get(1);
        assertEquals(BUS, bus.getMode());
        assertEquals("30", bus.getRoute().getShortName());

        var transfer = itin.legs.get(2);
        assertEquals(TraverseMode.WALK, transfer.getMode());

        var flex = itin.legs.get(3);
        assertEquals(BUS, flex.getMode());
        assertEquals("Zone 2", flex.getRoute().getShortName());
        assertTrue(flex.isFlexibleTrip());
    }

    @Test
    public void shouldReturnARouteWithTwoTransfers() {
        var from = GenericLocation.fromStopId("ALEX DR@ALEX WAY", "MARTA", "97266");
        var to = new GenericLocation(33.86701256815635, -84.61787939071655);

        var itin = getItinerary(from, to, 1);

        assertEquals(5, itin.legs.size());

        var firstBus = itin.legs.get(0);
        assertEquals(BUS, firstBus.getMode());
        assertEquals("856", firstBus.getRoute().getShortName());

        var transferToSecondBus = itin.legs.get(1);
        assertEquals(WALK, transferToSecondBus.getMode());

        var secondBus = itin.legs.get(2);
        assertEquals(BUS, secondBus.getMode());
        assertEquals("30", secondBus.getRoute().getShortName());

        var transferToFlex = itin.legs.get(3);
        assertEquals(WALK, transferToFlex.getMode());

        var finalFlex = itin.legs.get(4);
        assertEquals(BUS, finalFlex.getMode());
        assertEquals("Zone 2", finalFlex.getRoute().getShortName());
        assertTrue(finalFlex.isFlexibleTrip());
    }

    private Itinerary getItinerary(GenericLocation from, GenericLocation to, int index) {
        RoutingRequest request = new RoutingRequest();
        request.setDateTime(dateTime);
        request.from = from;
        request.to = to;
        request.numItineraries = 10;
        request.searchWindow = Duration.ofHours(2);
        request.modes.egressMode = FLEXIBLE;

        var result = service.route(request, router);
        var itineraries = result.getTripPlan().itineraries;

        assertFalse(itineraries.isEmpty());

        return itineraries.get(index);
    }

    private static void addGtfsToGraph(
            Graph graph,
            List<String> gtfsFiles
    ) {
        var extra = new HashMap<Class<?>, Object>();

        // GTFS
        var gtfsBundles = gtfsFiles.stream()
                .map(f -> new GtfsBundle(new File(f)))
                .collect(Collectors.toList());
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles, ServiceDateInterval.unbounded());
        gtfsModule.buildGraph(graph, extra);

        // link stations to streets
        StreetLinkerModule streetLinkerModule = new StreetLinkerModule();
        streetLinkerModule.buildGraph(graph, extra);

        // link flex locations to streets
        var flexMapper = new FlexLocationsToStreetEdgesMapper();
        flexMapper.buildGraph(graph, new HashMap<>());

        // generate direct transfers
        var req = new RoutingRequest();

        // we don't have a complete coverage of the entire area so use straight lines for transfers
        var transfers = new DirectTransferGenerator(600, List.of(req));
        transfers.buildGraph(graph, extra);

        graph.index();
    }


    @AfterAll
    static void teardown() {
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
    }
}
