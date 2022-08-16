package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;


import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model.site.FareZone;


public class HSLFareServiceTest implements PlanTestConstants{

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("createTestCases")
  public void canCalculateHSLFares(
    String testCaseName, // used to create parameterized test name
    FareService fareService,
    Itinerary i,
    FareAttribute expectedFare
  ) {
    Assertions.assertEquals(
      expectedFare.getId(),
      fareService.getCost(i).getDetails(Fare.FareType.regular).get(0).fareId
    );
    System.out.println("=================");
  }

  private static List<Arguments> createTestCases() {

    List<Arguments> args = new LinkedList<>();


    FareZone A = FareZone.of(new FeedScopedId(FEED_ID, "A")).build();
    FareZone B = FareZone.of(new FeedScopedId(FEED_ID, "B")).build();
    FareZone C = FareZone.of(new FeedScopedId(FEED_ID, "C")).build();
    FareZone D = FareZone.of(new FeedScopedId(FEED_ID, "D")).build();

    Place A1 = PlanTestConstants.place("A1", 10.0, 12.0, A);
    Place A2 = PlanTestConstants.place("A2", 10.0, 12.0, A);


    Place B1 = PlanTestConstants.place("B1", 10.0, 12.0, B);
    Place B2 = PlanTestConstants.place("B2", 10.0, 12.0, B);

    Place C1 = PlanTestConstants.place("C1", 10.0, 12.0, C);
    Place C2 = PlanTestConstants.place("C2", 10.0, 12.0, C);

    Place D1 = PlanTestConstants.place("D1", 10.0, 12.0, D);
    Place D2 = PlanTestConstants.place("D2", 10.0, 12.0, D);

    float AB_PRICE = 2.80f;
    float BC_PRICE = 2.80f;
    float CD_PRICE = 3.20f;

    float ABC_PRICE = 4.10f;
    float BCD_PRICE = 4.10f;

    float ABCD_PRICE = 5.70f;
    float D_PRICE = 2.80f;



    HSLFareServiceImpl hslFareService = new HSLFareServiceImpl();

    // Fare attributes
    FareAttribute fareAttributeAB = new FareAttribute(new FeedScopedId(FEED_ID, "AB"));
    FareAttribute fareAttributeBC = new FareAttribute(new FeedScopedId(FEED_ID, "BC"));
    FareAttribute fareAttributeCD = new FareAttribute(new FeedScopedId(FEED_ID, "CD"));

    FareAttribute fareAttributeABC = new FareAttribute(new FeedScopedId(FEED_ID, "ABC"));
    FareAttribute fareAttributeBCD = new FareAttribute(new FeedScopedId(FEED_ID, "BCD"));
    FareAttribute fareAttributeABCD = new FareAttribute(new FeedScopedId(FEED_ID, "ABCD"));
    FareAttribute fareAttributeD = new FareAttribute(new FeedScopedId(FEED_ID, "D"));

    // Currencies and prices
    fareAttributeAB.setCurrencyType("EUR");
    fareAttributeBC.setCurrencyType("EUR");
    fareAttributeCD.setCurrencyType("EUR");

    fareAttributeABC.setCurrencyType("EUR");
    fareAttributeBCD.setCurrencyType("EUR");
    fareAttributeABCD.setCurrencyType("EUR");
    fareAttributeD.setCurrencyType("EUR");

    fareAttributeAB.setPrice(AB_PRICE);
    fareAttributeBC.setPrice(BC_PRICE);
    fareAttributeCD.setPrice(CD_PRICE);

    fareAttributeABC.setPrice(ABC_PRICE);
    fareAttributeBCD.setPrice(BCD_PRICE);

    fareAttributeABCD.setPrice(ABCD_PRICE);
    fareAttributeD.setPrice(D_PRICE);

    // Fare rule sets
    FareRuleSet ruleSetAB = new FareRuleSet(fareAttributeAB);
    ruleSetAB.addContains("A");
    ruleSetAB.addContains("B");

    FareRuleSet ruleSetBC = new FareRuleSet(fareAttributeBC);
    ruleSetBC.addContains("B");
    ruleSetBC.addContains("C");

    FareRuleSet ruleSetCD = new FareRuleSet(fareAttributeCD);
    ruleSetCD.addContains("C");
    ruleSetCD.addContains("D");

    FareRuleSet ruleSetABC = new FareRuleSet(fareAttributeABC);
    ruleSetABC.addContains("A");
    ruleSetABC.addContains("B");
    ruleSetABC.addContains("C");

    FareRuleSet ruleSetBCD = new FareRuleSet(fareAttributeBCD);
    ruleSetBCD.addContains("B");
    ruleSetBCD.addContains("C");
    ruleSetBCD.addContains("D");

    FareRuleSet ruleSetABCD = new FareRuleSet(fareAttributeABCD);
    ruleSetABCD.addContains("A");
    ruleSetABCD.addContains("B");
    ruleSetABCD.addContains("C");
    ruleSetABCD.addContains("D");

    FareRuleSet ruleSetD = new FareRuleSet(fareAttributeD);
    ruleSetD.addContains("D");


    hslFareService.addFareRules(Fare.FareType.regular,
      List.of(
        ruleSetAB,
        ruleSetBC,
        ruleSetCD,
        ruleSetABC,
        ruleSetBCD,
        ruleSetABCD,
        ruleSetD
      )
    );

    // Itineraries within zone A
    Itinerary A1_A2 = newItinerary(A1, T11_06)
      .bus(1, T11_06, T11_12, A2)
      .build();

    args.add(
      Arguments.of("Bus ride within zone A", hslFareService, A1_A2, fareAttributeAB)
    );


    // Itineraries within zone B
    Itinerary B1_B2 = newItinerary(B1, T11_06)
      .bus(1, T11_06, T11_12, B2)
      .build();

    args.add(
      Arguments.of("Bus ride within zone B", hslFareService, B1_B2, fareAttributeAB)
    );


    // Itineraries within zone C
    Itinerary C1_C2 = newItinerary(C1, T11_06)
      .bus(1, T11_06, T11_12, C2)
      .build();

    args.add(
      Arguments.of("Bus ride within zone C", hslFareService, C1_C2, fareAttributeBC)
    );

    // Itineraries within zone D
    Itinerary D1_D2 = newItinerary(D1, T11_06)
      .bus(1, T11_06, T11_12, D2)
      .build();

    args.add(
      Arguments.of("Bus ride within zone D", hslFareService, D1_D2, fareAttributeD)
    );

    // Itineraries between zones A and B
    Itinerary A1_B1 = newItinerary(A1, T11_06)
      .bus(1, T11_06, T11_12, B1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones A and B", hslFareService, A1_B1, fareAttributeAB)
    );

    // Itineraries between zones B and C
    Itinerary B1_C1 = newItinerary(B1, T11_06)
      .bus(1, T11_06, T11_12, C1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones B and C", hslFareService, B1_C1, fareAttributeBC)
    );

    // Itineraries between zones C and D
    Itinerary C1_D1 = newItinerary(C1, T11_06)
      .bus(1, T11_06, T11_12, D1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones C and D", hslFareService, C1_D1, fareAttributeCD)
    );

    // Itineraries between zones A and D
    Itinerary A1_D1 = newItinerary(A1, T11_06)
      .bus(1,T11_20, T11_30, D1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones A and D", hslFareService, A1_D1, fareAttributeABCD)
    );


    // Itineraries between zones A and C
    Itinerary A1_C1 = newItinerary(A1, T11_06)
      .bus(1, T11_20, T11_30, C1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones A and C", hslFareService, A1_C1, fareAttributeABC)
    );

    // Itineraries between zones B and D
    Itinerary B1_D1 = newItinerary(B1, T11_06)
      .bus(1, T11_20, T11_30, D1)
      .build();

    args.add(
      Arguments.of("Bus ride between zones B and D", hslFareService, B1_D1, fareAttributeBCD)
    );


    return args;
  }

}