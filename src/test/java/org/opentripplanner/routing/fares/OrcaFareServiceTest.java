package org.opentripplanner.routing.fares;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.OrcaFareServiceFactory;
import org.opentripplanner.routing.impl.OrcaFareServiceImpl;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;

public class OrcaFareServiceTest {

    private static final float regularCashFare = 3.50f;
    private static final float youthCashFare = 2.50f;
    private static OrcaFareServiceImpl orcaFareService;
    private static Agency agency;
    private static Route route;

    @BeforeAll
    public static void setUpClass() {
        orcaFareService = new OrcaFareServiceImpl();
        agency = new Agency();
        route = new Route();
    }

    @ParameterizedTest
    @MethodSource("createRoutes")
    public void correctOrcaLegFareIsAppliedTests(String agencyName,
                                                 int routeType,
                                                 String routeDesc,
                                                 Fare.FareType fareType,
                                                 Float expectedFare
    ) {
        agency.setName(agencyName);
        route.setAgency(agency);
        route.setType(routeType);
        route.setDesc(routeDesc);
        Float orcaFare = orcaFareService.calcFare(route, regularCashFare, youthCashFare, fareType);
        Assertions.assertEquals(orcaFare, expectedFare);
    }

    private static Stream<Arguments> createRoutes() {
        return Stream.of(
            Arguments.of("Community Transit", 3, "", Fare.FareType.orcaLift, 1.25f),
            Arguments.of("Community Transit", 1, "", Fare.FareType.orcaLift, 2f),
            Arguments.of("Community Transit", 3, "", Fare.FareType.orcaReduced, 1.25f),
            Arguments.of("Community Transit", 1, "", Fare.FareType.orcaReduced, 2f),
            Arguments.of("Community Transit", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("Community Transit", -1, "", Fare.FareType.orcaYouth, youthCashFare),
            Arguments.of("Metro Transit", 4, "Water Taxi: West Seattle", Fare.FareType.orcaLift, youthCashFare),
            Arguments.of("Metro Transit", 4, "Water Taxi: West Seattle", Fare.FareType.orcaReduced, 2.5f),
            Arguments.of("Metro Transit", 4, "Water Taxi: West Seattle", Fare.FareType.orcaRegular, 5f),
            Arguments.of("Metro Transit", 4, "Water Taxi: West Seattle", Fare.FareType.orcaYouth, 3.75f),
            Arguments.of("Metro Transit", 4, "Water Taxi: Vashon Island", Fare.FareType.orcaLift, youthCashFare),
            Arguments.of("Metro Transit", 4, "Water Taxi: Vashon Island", Fare.FareType.orcaReduced, 3f),
            Arguments.of("Metro Transit", 4, "Water Taxi: Vashon Island", Fare.FareType.orcaRegular, 5.75f),
            Arguments.of("Metro Transit", 4, "Water Taxi: Vashon Island", Fare.FareType.orcaYouth, 4.5f),
            Arguments.of("Sound Transit", -1, "", Fare.FareType.orcaLift, youthCashFare),
            Arguments.of("Sound Transit", -1, "", Fare.FareType.orcaReduced, 1f),
            Arguments.of("Sound Transit", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("Sound Transit", -1, "", Fare.FareType.orcaYouth, youthCashFare),
            Arguments.of("Everett Transit", -1, "", Fare.FareType.orcaLift, youthCashFare),
            Arguments.of("Everett Transit", -1, "", Fare.FareType.orcaReduced, 0.5f),
            Arguments.of("Everett Transit", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("Everett Transit", -1, "", Fare.FareType.orcaYouth, youthCashFare),
            Arguments.of("Pierce Transit", -1, "", Fare.FareType.orcaLift, null),
            Arguments.of("Pierce Transit", -1, "", Fare.FareType.orcaReduced, 1f),
            Arguments.of("Pierce Transit", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("Pierce Transit", -1, "", Fare.FareType.orcaYouth, youthCashFare),
            Arguments.of("City of Seattle", -1, "", Fare.FareType.orcaLift, youthCashFare),
            Arguments.of("City of Seattle", -1, "", Fare.FareType.orcaReduced, 1f),
            Arguments.of("City of Seattle", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("City of Seattle", -1, "", Fare.FareType.orcaYouth, youthCashFare),
            Arguments.of("Washington State Ferries", -1, "", Fare.FareType.orcaLift, null),
            Arguments.of("Washington State Ferries", -1, "", Fare.FareType.orcaReduced, null),
            Arguments.of("Washington State Ferries", -1, "", Fare.FareType.orcaRegular, null),
            Arguments.of("Washington State Ferries", -1, "", Fare.FareType.orcaYouth, null),
            Arguments.of("Kitsap Transit", -1, "", Fare.FareType.orcaLift, 1f),
            Arguments.of("Kitsap Transit", -1, "", Fare.FareType.orcaReduced, 1f),
            Arguments.of("Kitsap Transit", -1, "", Fare.FareType.orcaRegular, regularCashFare),
            Arguments.of("Kitsap Transit", -1, "", Fare.FareType.orcaYouth, youthCashFare)
        );
    }

    // TODO: Define test routes and read in required GTFS feeds.
    @Test
    public void test() throws Exception {
        AStar aStar = new AStar();
        Graph gg = new Graph();
        GtfsContext KCcontext = GtfsLibrary.readGtfs(new File(ConstantsForTests.KCM_GTFS));
        GtfsContext CalContext = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));
        // Add - wash state and commtrans GTFS feeds.

        // create one of these for each GTFS feed.
        PatternHopFactory KCfactory = new PatternHopFactory(KCcontext);
        KCfactory.setFareServiceFactory(new OrcaFareServiceFactory());
        PatternHopFactory Calfactory = new PatternHopFactory(KCcontext);
        Calfactory.setFareServiceFactory(new OrcaFareServiceFactory());

        KCfactory.run(gg);
        Calfactory.run(gg);
        gg.putService(
            CalendarServiceData.class,
            createCalendarServiceData(KCcontext.getOtpTransitService())
        );
//        gg.putService(
//            CalendarServiceData.class,
//            createCalendarServiceData(CalContext.getOtpTransitService())
//        );
        FareService fareService = gg.getService(FareService.class);

        RoutingRequest options = new RoutingRequest();
        String feedId = gg.getFeedIds().iterator().next();

        String vertex0 = feedId + ":2010";
        String vertex1 = feedId + ":2140";

        options.dateTime = TestUtils.dateInSeconds("America/Los_Angeles", 2016, 5, 24, 5, 0, 0);
        options.setRoutingContext(gg, vertex0, vertex1);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(gg.getVertex(vertex1), true);

        Fare fare = fareService.getCost(path);
        Assertions.assertEquals(6, fare.fare.size());
    }


}
