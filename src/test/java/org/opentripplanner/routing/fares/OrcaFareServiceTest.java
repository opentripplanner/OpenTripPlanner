package org.opentripplanner.routing.fares;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.impl.OrcaFareServiceImpl;
import org.opentripplanner.routing.impl.Ride;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.WASHINGTON_STATE_FERRIES_AGENCY_ID;

//TODO: Add additional test routes once defined by Jon.
public class OrcaFareServiceTest {

    private static OrcaFareServiceImpl orcaFareService;
    private static float DEFAULT_RIDE_PRICE_IN_CENTS;

    @BeforeAll
    public static void setUpClass() {
        Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
        orcaFareService = new OrcaFareServiceImpl(regularFareRules.values());
        orcaFareService.IS_TEST = true;
        DEFAULT_RIDE_PRICE_IN_CENTS = OrcaFareServiceImpl.DEFAULT_TEST_RIDE_PRICE * 100;
    }

    /**
     * These tests are designed to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable
     * to make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
     * substitute {@link OrcaFareServiceImpl#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
     * types.
     */
    private static void calculateFare(List<Ride> rides,
                                  Fare.FareType fareType,
                                  float expectedFareInCents
    ) {
        Fare fare = new Fare();
        orcaFareService.populateFare(fare, null, fareType, rides, null);
        Assertions.assertEquals(expectedFareInCents, fare.getFare(fareType).getCents());
    }

    /**
     * Test to confirm the correct transfer cost per fare type within a single agency.
     */
    @Test
    public void calculateFareForSingleAgency() {
        Ride ride = getRide(COMM_TRANS_AGENCY_ID, "400", 0);
        List<Ride> rides = Lists.newArrayList(ride);
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSpecial, 200f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 200f);
        calculateFare(rides, Fare.FareType.electronicYouth, 300f);
    }

    /**
     * WSF do not except free transfers. This test is to make sure the rider is charged the cash price for WSF as well
     * as the highest fare where Orca can be used.
     */
    @Test
    public void calculateFareWithNoFreeTransfer() {
        List<Ride> rides = Lists.newArrayList();
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 0));
        rides.add(getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 1));
        rides.add(getRide(COMM_TRANS_AGENCY_ID, 2));
        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
        calculateFare(rides, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
        calculateFare(rides, Fare.FareType.electronicSpecial, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
        calculateFare(rides, Fare.FareType.electronicRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
        calculateFare(rides, Fare.FareType.electronicYouth, 200f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f);
    }

    /**
     * Total trip time is 2h 30m. The first four transfers are within the permitted two hour window. A single (highest)
     * Orca fare will be charged for these transfers. The fifth transfer is outside of the two hour window and will be
     * charged a cash rate. At this point, the two hour window will start again so the final transfer will be
     * charged at a discount rate... if using Orca.
     */
    @Test
    public void calculateFareThatExceedsTwoHourFreeTransferWindow() {
        List<Ride> rides = Lists.newArrayList();
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 0));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 30));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 60));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 90));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 120));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 150));

        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.electronicSpecial, 100f + 0f + 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 100f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f +
            DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS);
        calculateFare(rides, Fare.FareType.electronicSenior, 100f + 0f + 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 100f);
        calculateFare(rides, Fare.FareType.electronicYouth, 200f + 0f + 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 200f);
    }

    /**
     * This trip starts with a cash fare so the free transfer window doesn't start until the second transfer. Therefore,
     * all subsequent transfers will come under one transfer window and only one Orca discount charge will apply.
     */
    @Test
    public void calculateFareThatStartsWithACashFare() {
        List<Ride> rides = Lists.newArrayList();
        rides.add(getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 30));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 60));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 90));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 120));
        rides.add(getRide(KITSAP_TRANSIT_AGENCY_ID, 149));

        calculateFare(rides, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
        calculateFare(rides, Fare.FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS +
            DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicSenior, DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f);
        calculateFare(rides, Fare.FareType.electronicYouth, DEFAULT_RIDE_PRICE_IN_CENTS + 200f + 0f + 0f + 0f + 0f);
    }

    private static Ride getRide(String agencyId, long startTimeMins) {
        return getRide(agencyId, "-1", -1, null, startTimeMins);
    }

    private static Ride getRide(String agencyId, String shortName, long startTimeMins) {
        return getRide(agencyId, shortName, -1, null,startTimeMins);
    }

    /**
     * Create a {@link Ride} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
     * correct ride type.
     */
    private static Ride getRide(String agencyId, String shortName, int rideType, String desc, long startTimeMins) {
        Ride ride = new Ride();
        Agency agency = new Agency();
        agency.setId(agencyId);
        Route route = new Route();
        route.setAgency(agency);
        route.setShortName(shortName);
        route.setType(rideType);
        route.setDesc(desc);
        ride.routeData = route;
        ride.startTime = startTimeMins * 60;
        return ride;
    }
}
