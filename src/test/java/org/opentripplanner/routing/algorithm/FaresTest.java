package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.raptor.router.TransitRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.impl.SeattleFareServiceFactory;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.TestUtils;

public class FaresTest {

    private final WrappedCurrency USD = new WrappedCurrency("USD");
    private AStar aStar = new AStar();

    @Test
    public void testBasic() {

        var graph = ConstantsForTests.buildGtfsGraph(ConstantsForTests.CALTRAIN_GTFS);

        var feedId = graph.getFeedIds().iterator().next();

        var router = new Router(graph, RouterConfig.DEFAULT);
        router.startup();

        var start = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 0, 0);
        var from = GenericLocation.fromStopId("Origin", feedId, "Millbrae Caltrain");
        var to = GenericLocation.fromStopId("Destination", feedId, "Mountain View Caltrain");

        Fare fare = getFare(from, to, start, router);
        assertEquals(fare.getFare(FareType.regular), new Money(USD, 425));
    }

    @Test
    public void testPortland() {

        Graph graph = ConstantsForTests.getInstance().getCachedPortlandGraph();
        var portlandId = graph.getFeedIds().iterator().next();

        var router = new Router(graph, RouterConfig.DEFAULT);
        router.startup();

        // from zone 3 to zone 2
        var from = GenericLocation.fromStopId(
                "Portland Int'l Airport MAX Station,Eastbound stop in Portland",
                portlandId, "10579"
        );
        var to = GenericLocation.fromStopId("NE 82nd Ave MAX Station,Westbound stop in Portland",
                portlandId, "8371"
        );

        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 0, 0);

        Fare fare = getFare(from, to, startTime, router);

        assertEquals(new Money(USD, 200), fare.getFare(FareType.regular));

        // long trip

        startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 14, 0, 0);

        from = GenericLocation.fromStopId("Origin", portlandId, "8389");
        to = GenericLocation.fromStopId("Destination", portlandId, "1252");

        fare = getFare(from, to, startTime, router);

        // this assertion was already commented out when I reactivated the test for OTP2 on 2021-11-11
        // not sure what the correct fare should be
        // assertEquals(new Money(new WrappedCurrency("USD"), 460), fare.getFare(FareType.regular));

        // complex trip
        // request.maxTransfers = 5;
        // startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 14, 0, 0);
        // request.dateTime = startTime;
        // request.from = GenericLocation.fromStopId("", portlandId, "10428");
        // request.setRoutingContext(graph, portlandId + ":10428", portlandId + ":4231");

        //
        // this is commented out because portland's fares are, I think, broken in the gtfs. see
        // thread on gtfs-changes.
        // assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 430));
    }

    @Test
    public void testKCM() {

        Graph graph = ConstantsForTests.buildGtfsGraph(
                ConstantsForTests.KCM_GTFS,
                new SeattleFareServiceFactory()
        );
        var feedId = graph.getFeedIds().iterator().next();

        var router = new Router(graph, RouterConfig.DEFAULT);
        router.startup();

        var from = GenericLocation.fromStopId("Origin", feedId, "2010");
        var to = GenericLocation.fromStopId("Destination", feedId, "2140");

        var dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 5, 0, 0);

        var costOffPeak = getFare(from, to, dateTime, router);

        assertEquals(new Money(USD, 250), costOffPeak.getFare(FareType.regular));

        long onPeakStartTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 8, 0, 0);
        var peakItinerary = getItineraries(from, to, onPeakStartTime, router).get(1);
        var leg = peakItinerary.legs.get(0);
        assertTrue(toLocalTime(leg.startTime).isAfter(LocalTime.parse("08:00")));
        assertTrue(toLocalTime(leg.startTime).isBefore(LocalTime.parse("09:00")));

        assertEquals(new Money(USD, 275), peakItinerary.fare.getFare(FareType.regular));

    }

    public void testFareComponent() throws Exception {
        Graph graph = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FARE_COMPONENT_GTFS);
        String feedId = graph.getFeedIds().iterator().next();

        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 0, 0);
        ShortestPathTree spt;
        GraphPath path;
        Fare fare;
        List<FareComponent> fareComponents = null;
        FareService fareService = graph.getService(FareService.class);
        Money tenUSD = new Money(USD, 1000);

        // A -> B, base case
        options.setRoutingContext(graph, feedId + ":A", feedId + ":B");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":B"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "1"));

        // D -> E, null case
        options.setRoutingContext(graph, feedId + ":D", feedId + ":E");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":E"), true);
        fare = null; // was: fareService.getCost(path);
        assertNull(fare);

        // A -> C, 2 components in a path
        options.setRoutingContext(graph, feedId + ":A", feedId + ":C");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":C"), true);
        fare = null; // was:  fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 2);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "1"));
        assertEquals(fareComponents.get(1).price, tenUSD);
        assertEquals(fareComponents.get(1).fareId, new FeedScopedId(feedId, "BC"));
        assertEquals(fareComponents.get(1).routes.get(0), new FeedScopedId("agency", "2"));

        // B -> D, 2 fully connected components
        options.setRoutingContext(graph, feedId + ":B", feedId + ":D");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":D"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "BD"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "2"));
        assertEquals(fareComponents.get(0).routes.get(1), new FeedScopedId("agency", "3"));

        // E -> G, missing in between fare
        options.setRoutingContext(graph, feedId + ":E", feedId + ":G");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":G"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "EG"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "5"));
        assertEquals(fareComponents.get(0).routes.get(1), new FeedScopedId("agency", "6"));

        // C -> E, missing fare after
        options.setRoutingContext(graph, feedId + ":C", feedId + ":E");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":E"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "CD"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "3"));

        // D -> G, missing fare before
        options.setRoutingContext(graph, feedId + ":D", feedId + ":G");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":G"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 1);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "EG"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "5"));
        assertEquals(fareComponents.get(0).routes.get(1), new FeedScopedId("agency", "6"));

        // A -> D, use individual component parts
        options.setRoutingContext(graph, feedId + ":A", feedId + ":D");
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(graph.getVertex(feedId + ":D"), true);
        fare = null; // was: fareService.getCost(path);
        fareComponents = fare.getDetails(FareType.regular);
        assertEquals(fareComponents.size(), 2);
        assertEquals(fareComponents.get(0).price, tenUSD);
        assertEquals(fareComponents.get(0).fareId, new FeedScopedId(feedId, "AB"));
        assertEquals(fareComponents.get(0).routes.get(0), new FeedScopedId("agency", "1"));
        assertEquals(fareComponents.get(1).price, tenUSD);
        assertEquals(fareComponents.get(1).fareId, new FeedScopedId(feedId, "BD"));
        assertEquals(fareComponents.get(1).routes.get(0), new FeedScopedId("agency", "2"));
        assertEquals(fareComponents.get(1).routes.get(1), new FeedScopedId("agency", "3"));
    }

    private static Fare getFare(
            GenericLocation from,
            GenericLocation to,
            long time,
            Router router
    ) {
        Itinerary itinerary = getItineraries(from, to, time, router).get(0);
        return itinerary.fare;
    }

    private static List<Itinerary> getItineraries(
            GenericLocation from,
            GenericLocation to,
            long time,
            Router router
    ) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = time;
        request.from = from;
        request.to = to;

        var result = TransitRouter.route(request, router, new DebugTimingAggregator());
        return result.getItineraries();
    }

    private static LocalTime toLocalTime(Calendar time) {
        return time.toInstant()
                .atZone(time.getTimeZone().toZoneId())
                .toLocalTime();
    }
}

