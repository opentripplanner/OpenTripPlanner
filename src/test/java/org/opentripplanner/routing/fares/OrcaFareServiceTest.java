package org.opentripplanner.routing.fares;

import junit.framework.TestCase;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.OrcaFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

import java.io.File;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;

public class OrcaFareServiceTest extends TestCase {

    private AStar aStar = new AStar();

    public void test() throws Exception {
        Graph gg = new Graph();
        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.KCM_GTFS));

        OrcaFareServiceFactory orcaFareServiceFactory = new OrcaFareServiceFactory();
        orcaFareServiceFactory.processGtfs(context.getOtpTransitService());

        gg.putService(
            CalendarServiceData.class,
            createCalendarServiceData(context.getOtpTransitService())
        );

        RoutingRequest options = new RoutingRequest();
        String vertex0 = context.getFeedId().getId() + ":2010";
        String vertex1 = context.getFeedId().getId() + ":2140";

        options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 5, 0, 0);
        options.setRoutingContext(gg, vertex0, vertex1);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(gg.getVertex(vertex1), true);

        FareService f = orcaFareServiceFactory.makeFareService();
        Fare fare = f.getCost(path);
        assertEquals(5, fare.fare.size());
    }
}
