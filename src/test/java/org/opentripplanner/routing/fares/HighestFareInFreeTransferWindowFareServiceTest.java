package org.opentripplanner.routing.fares;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.impl.HighestFareInFreeTransferWindowFareServiceImpl;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.LinkedList;
import java.util.List;

public class HighestFareInFreeTransferWindowFareServiceTest {

    @ParameterizedTest
    @MethodSource("createTestCases")
    public void canCalculateFare(
        FareService fareService,
        GraphPath path,
        float expectedFare
    ) {
        Assertions.assertEquals(
            Math.round(expectedFare * 100),
            fareService.getCost(path).fare.get(Fare.FareType.regular).getCents()
        );
    }

    private static List<Arguments> createTestCases() {
        List<Arguments> args = new LinkedList();

        // create routes
        Route routeA = GraphPathBuilder.makeNewRoute(
            "A",
            "Route with one dollar fare"
        );
        Route routeB = GraphPathBuilder.makeNewRoute(
            "B",
            "Route with one dollar fare"
        );
        Route routeC = GraphPathBuilder.makeNewRoute(
            "C",
            "Route with two dollar fare"
        );

        // create fare attributes and rules
        List<FareRuleSet> defaultFareRules = new LinkedList<>();

        // $1 fares
        float oneDollar = 1.0f;
        FareAttribute oneDollarFareAttribute = new FareAttribute();
        oneDollarFareAttribute.setId(new FeedScopedId(GraphPathBuilder.FEED_ID, "oneDollarAttribute"));
        oneDollarFareAttribute.setCurrencyType("USD");
        oneDollarFareAttribute.setPrice(oneDollar);
        FareRuleSet oneDollarRouteBasedFares = new FareRuleSet(oneDollarFareAttribute);
        oneDollarRouteBasedFares.addRoute(routeA.getId());
        oneDollarRouteBasedFares.addRoute(routeB.getId());
        defaultFareRules.add(oneDollarRouteBasedFares);

        // $2 fares
        float twoDollars = 2.0f;
        FareAttribute twoDollarFareAttribute = new FareAttribute();
        twoDollarFareAttribute.setId(new FeedScopedId(GraphPathBuilder.FEED_ID, "twoDollarAttribute"));
        twoDollarFareAttribute.setCurrencyType("USD");
        twoDollarFareAttribute.setPrice(twoDollars);
        FareRuleSet twoDollarRouteBasedFares = new FareRuleSet(twoDollarFareAttribute);
        twoDollarRouteBasedFares.addRoute(routeC.getId());
        defaultFareRules.add(twoDollarRouteBasedFares);

        FareService defaultFareService = new HighestFareInFreeTransferWindowFareServiceImpl(
            defaultFareRules,
            150,
            false
        );

        // a single transit leg should calculate default fare
        GraphPath singleTransitLegPath = new GraphPathBuilder()
            .addTransitLeg(routeA, 30, 60)
            .build();
        args.add(Arguments.of(defaultFareService, singleTransitLegPath, oneDollar));

        // a two transit leg itinerary with same route costs within free-transfer window should calculate default fare
        GraphPath twoTransitLegPath = new GraphPathBuilder()
            .addTransitLeg(routeA, 30, 60)
            .addTransitLeg(routeB, 90, 120)
            .build();
        args.add(Arguments.of(defaultFareService, twoTransitLegPath, oneDollar));

        // a two transit leg itinerary where the first route costs more than the second route, but both are within
        // the free transfer window should calculate the first route cost
        GraphPath twoTransitLegFirstRouteCostsTwoDollarsPath = new GraphPathBuilder()
            .addTransitLeg(routeC, 30, 60)
            .addTransitLeg(routeB, 90, 120)
            .build();
        args.add(Arguments.of(defaultFareService, twoTransitLegFirstRouteCostsTwoDollarsPath, twoDollars));

        // a two transit leg itinerary where the second route costs more than the first route, but both are within
        // the free transfer window should calculate the second route cost
        GraphPath twoTransitLegSecondRouteCostsTwoDollarsPath = new GraphPathBuilder()
            .addTransitLeg(routeB, 30, 60)
            .addTransitLeg(routeC, 90, 120)
            .build();
        args.add(Arguments.of(defaultFareService, twoTransitLegSecondRouteCostsTwoDollarsPath, twoDollars));

        // a two transit leg itinerary with same route costs but where the second leg begins after the free transfer
        // window should calculate double the default fare
        GraphPath twoTransitLegSecondRouteHappensAfterFreeTransferWindowPath = new GraphPathBuilder()
            .addTransitLeg(routeA, 30, 60)
            .addTransitLeg(routeB, 10000, 10120)
            .build();
        args.add(
            Arguments.of(
                defaultFareService,
                twoTransitLegSecondRouteHappensAfterFreeTransferWindowPath,
                oneDollar + oneDollar
            )
        );

        // a three transit leg itinerary with same route costs but where the second leg begins after the free transfer
        // window, but the third leg is within the second free transfer window should calculate double the default fare
        GraphPath threeTransitLegSecondRouteHappensAfterFreeTransferWindowPath = new GraphPathBuilder()
            .addTransitLeg(routeA, 30, 60)
            .addTransitLeg(routeB, 10000, 10120)
            .addTransitLeg(routeA, 10150, 10180)
            .build();
        args.add(
            Arguments.of(
                defaultFareService,
                threeTransitLegSecondRouteHappensAfterFreeTransferWindowPath,
                oneDollar + oneDollar
            )
        );

        // a two transit leg itinerary with an interlined transfer where the second route costs more than the first
        // route, but both are within the free transfer window should calculate the first route cost
        GraphPath twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath = new GraphPathBuilder()
            .addInterlinedTransitLeg(routeB, 30, 60)
            .addTransitLeg(routeC, 90, 120)
            .build();
        args.add(Arguments.of(defaultFareService, twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath, oneDollar));

        // a two transit leg itinerary with an interlined transfer where the second route costs more than the first
        // route, but both are within the free transfer window and the fare service is configured with the
        // analyzeInterlinedTransfers set to true should calculate the second route cost
        FareService fareServiceWithAnalyzedInterlinedTransfersConfig =
            new HighestFareInFreeTransferWindowFareServiceImpl(
                defaultFareRules,
                150,
                true
            );

        args.add(
            Arguments.of(
                fareServiceWithAnalyzedInterlinedTransfersConfig,
                twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath,
                twoDollars
            )
        );

        return args;
    }
}
