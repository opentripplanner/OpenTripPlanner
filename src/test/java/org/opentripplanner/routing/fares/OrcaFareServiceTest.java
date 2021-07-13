package org.opentripplanner.routing.fares;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.impl.OrcaFareServiceImpl;
import org.opentripplanner.routing.impl.Ride;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.COMM_TRANS_COMMUTER_EXPRESS;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.COMM_TRANS_LOCAL_SWIFT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.EVERETT_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KC_METRO;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KC_WATER_TAXI_VASHON_ISLAND;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KITSAP_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.PIERCE_COUNTY_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.SEATTLE_STREET_CAR;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.SOUND_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.WASHINGTON_STATE_FERRIES;

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
     * These test are design to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable to
     * make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
     * substitute {@link OrcaFareServiceImpl#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
     * types.
     */
    @ParameterizedTest
    @MethodSource("trips")
    public void calculateFareTest(List<Ride> rides,
                                  Fare.FareType fareType,
                                  float expectedFareInCents
    ) {
        Fare fare = new Fare();
        orcaFareService.calculateFare(fare, null, fareType, rides, null);
        Assertions.assertEquals(expectedFareInCents, fare.getFare(fareType).getCents());
    }

    private static Stream<Arguments> trips() {
        List<Ride> trip1 = getTrip(
            COMM_TRANS_COMMUTER_EXPRESS
        );
        List<Ride> trip2 = getTrip(
            KITSAP_TRANSIT,
            WASHINGTON_STATE_FERRIES,
            COMM_TRANS_LOCAL_SWIFT
        );
        List<Ride> trip3 = getTrip(
            PIERCE_COUNTY_TRANSIT,
            SOUND_TRANSIT,
            COMM_TRANS_COMMUTER_EXPRESS
        );
        List<Ride> trip4 = getTrip(
            EVERETT_TRANSIT,
            COMM_TRANS_COMMUTER_EXPRESS,
            SOUND_TRANSIT
        );
        List<Ride> trip5 = getTrip(
            KC_WATER_TAXI_VASHON_ISLAND,
            KC_METRO
        );
        List<Ride> trip6 = getTrip(
            SEATTLE_STREET_CAR,
            KC_METRO,
            COMM_TRANS_COMMUTER_EXPRESS
        );
        return Stream.of(
            Arguments.of(trip1, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.orcaLift, 200f),
            Arguments.of(trip1, Fare.FareType.orcaRegular, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.orcaSenior, 200f),
            Arguments.of(trip1, Fare.FareType.orcaYouth, 300f),
            // The following costs are made up of the expected cost to be applied for each agency. This is usually the
            // most expensive leg of the journey.
            Arguments.of(trip2, Fare.FareType.orcaLift, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f),
            Arguments.of(trip2, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip2, Fare.FareType.orcaSenior, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f),
            Arguments.of(trip2, Fare.FareType.orcaYouth, 200f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip3, Fare.FareType.orcaLift, DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f),
            Arguments.of(trip3, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip3, Fare.FareType.orcaSenior, 0f + 0f + 200f),
            Arguments.of(trip3, Fare.FareType.orcaYouth, 0f + 0f + 300f),
            Arguments.of(trip4, Fare.FareType.orcaLift, 0f + 200f + 0f),
            Arguments.of(trip4, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip4, Fare.FareType.orcaSenior, 0f + 0f + 200f),
            Arguments.of(trip4, Fare.FareType.orcaYouth, 0f + 0f + 300f),
            Arguments.of(trip5, Fare.FareType.orcaLift, 450f + 0f),
            Arguments.of(trip5, Fare.FareType.orcaRegular, 575f + 0f),
            Arguments.of(trip5, Fare.FareType.orcaSenior, 300f + 0f),
            Arguments.of(trip5, Fare.FareType.orcaYouth, 450f + 0f),
            Arguments.of(trip6, Fare.FareType.orcaLift, 0f + 0f + 200f),
            Arguments.of(trip6, Fare.FareType.orcaRegular, 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip6, Fare.FareType.orcaSenior, 0f + 0f + 200f),
            Arguments.of(trip6, Fare.FareType.orcaYouth, 0f + 0f + 300f)
        );
    }

    /**
     * Build a list of {@link Ride)s from the ride types provided. The values used here to produce a {@link Ride} match
     * the values used in {@link OrcaFareServiceImpl} to determine the correct ride type.
     */
    private static List<Ride> getTrip(OrcaFareServiceImpl.RideType... rideTypes) {
        List<Ride> rides = new ArrayList<>();
        for (OrcaFareServiceImpl.RideType rideType : rideTypes) {
            switch (rideType) {
                case COMM_TRANS_LOCAL_SWIFT:
                    rides.add(getRide("29"));
                    break;
                case COMM_TRANS_COMMUTER_EXPRESS:
                    rides.add(getRide("29", "400"));
                    break;
                case EVERETT_TRANSIT:
                    rides.add(getRide("97"));
                    break;
                case PIERCE_COUNTY_TRANSIT:
                    rides.add(getRide("3"));
                    break;
                case SEATTLE_STREET_CAR:
                    rides.add(getRide("23"));
                    break;
                case KITSAP_TRANSIT:
                    rides.add(getRide("kt"));
                    break;
                case KC_WATER_TAXI_VASHON_ISLAND:
                    rides.add(getRide("1", 4, "Water Taxi: Vashon Island"));
                    break;
                case KC_WATER_TAXI_WEST_SEATTLE:
                    rides.add(getRide("1", 4, "Water Taxi: West Seattle"));
                    break;
                case KC_METRO:
                    rides.add(getRide("1"));
                    break;
                case SOUND_TRANSIT:
                    rides.add(getRide("40"));
                    break;
                case WASHINGTON_STATE_FERRIES:
                    rides.add(getRide("wsf"));
                    break;
            }

        }
        return rides;
    }

    private static Ride getRide(String agencyId) {
        return getRide(agencyId, "-1", -1, null);
    }

    private static Ride getRide(String agencyId, int rideType, String desc) {
        return getRide(agencyId, "-1", rideType, desc);
    }

    private static Ride getRide(String agencyId, String shortName) {
        return getRide(agencyId, shortName, -1, null);
    }

    /**
     * Create a {@link Ride} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
     * correct ride type.
     */
    private static Ride getRide(String agencyId, String shortName, int rideType, String desc) {
        Ride ride = new Ride();
        Agency agency = new Agency();
        agency.setId(agencyId);
        Route route = new Route();
        route.setAgency(agency);
        route.setShortName(shortName);
        route.setType(rideType);
        route.setDesc(desc);
        ride.routeData = route;
        return ride;
    }
}
