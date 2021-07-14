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
import java.util.function.Function;
import java.util.stream.Stream;

import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.EVERETT_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.KC_METRO_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.PIERCE_COUNTY_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.ROUTE_TYPE_FERRY;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.COMM_TRANS_COMMUTER_EXPRESS;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.COMM_TRANS_LOCAL_SWIFT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.EVERETT_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KC_METRO;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KC_WATER_TAXI_VASHON_ISLAND;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KC_WATER_TAXI_WEST_SEATTLE;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.KITSAP_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.PIERCE_COUNTY_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.SEATTLE_STREET_CAR;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.SOUND_TRANSIT;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.RideType.WASHINGTON_STATE_FERRIES;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.SEATTLE_STREET_CAR_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.SOUND_TRANSIT_AGENCY_ID;
import static org.opentripplanner.routing.impl.OrcaFareServiceImpl.WASHINGTON_STATE_FERRIES_AGENCY_ID;

public class OrcaFareServiceTest {

    private static OrcaFareServiceImpl orcaFareService;
    private static float DEFAULT_RIDE_PRICE_IN_CENTS;

    private static final Map<OrcaFareServiceImpl.RideType, Ride> tripStrategy = new HashMap<>();

    @BeforeAll
    public static void setUpClass() {
        Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
        orcaFareService = new OrcaFareServiceImpl(regularFareRules.values());
        orcaFareService.IS_TEST = true;
        DEFAULT_RIDE_PRICE_IN_CENTS = OrcaFareServiceImpl.DEFAULT_TEST_RIDE_PRICE * 100;

        tripStrategy.put(COMM_TRANS_LOCAL_SWIFT, getRide(COMM_TRANS_AGENCY_ID));
        tripStrategy.put(COMM_TRANS_COMMUTER_EXPRESS, getRide(COMM_TRANS_AGENCY_ID, "400"));
        tripStrategy.put(EVERETT_TRANSIT, getRide(EVERETT_TRANSIT_AGENCY_ID));
        tripStrategy.put(PIERCE_COUNTY_TRANSIT, getRide(PIERCE_COUNTY_TRANSIT_AGENCY_ID));
        tripStrategy.put(SEATTLE_STREET_CAR, getRide(SEATTLE_STREET_CAR_AGENCY_ID));
        tripStrategy.put(KITSAP_TRANSIT, getRide(KITSAP_TRANSIT_AGENCY_ID));
        tripStrategy.put(
            KC_WATER_TAXI_VASHON_ISLAND,
            getRide(KC_METRO_AGENCY_ID, ROUTE_TYPE_FERRY, "Water Taxi: Vashon Island")
        );
        tripStrategy.put(
            KC_WATER_TAXI_WEST_SEATTLE,
            getRide(KC_METRO_AGENCY_ID, ROUTE_TYPE_FERRY, "Water Taxi: West Seattle")
        );
        tripStrategy.put(KC_METRO, getRide(KC_METRO_AGENCY_ID));
        tripStrategy.put(SOUND_TRANSIT, getRide(SOUND_TRANSIT_AGENCY_ID));
        tripStrategy.put(WASHINGTON_STATE_FERRIES, getRide(WASHINGTON_STATE_FERRIES_AGENCY_ID));
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
        orcaFareService.populateFare(fare, null, fareType, rides, null);
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
        // The cost parameters are made up of the expected fare to be applied after each leg of a trip has been evaluated.
        // E.g. if a trip covers three agencies a value of "0f + 0f + 200f" would represent no charge for the first two
        // legs, but a charge of 200 cents for the third. This implies that the third leg is the most expensive transfer
        // of the trip.
        return Stream.of(
            Arguments.of(trip1, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.orcaLift, 200f),
            Arguments.of(trip1, Fare.FareType.orcaRegular, DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip1, Fare.FareType.orcaSenior, 200f),
            Arguments.of(trip1, Fare.FareType.orcaYouth, 300f),
            Arguments.of(trip2, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip2, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip2, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip2, Fare.FareType.orcaLift, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f),
            Arguments.of(trip2, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS),
            Arguments.of(trip2, Fare.FareType.orcaSenior, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f),
            Arguments.of(trip2, Fare.FareType.orcaYouth, 200f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip3, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip3, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip3, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip3, Fare.FareType.orcaLift, DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f),
            Arguments.of(trip3, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip3, Fare.FareType.orcaSenior, 0f + 0f + 200f),
            Arguments.of(trip3, Fare.FareType.orcaYouth, 0f + 0f + 300f),
            Arguments.of(trip4, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip4, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip4, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 3),
            Arguments.of(trip4, Fare.FareType.orcaLift, 0f + 200f + 0f),
            Arguments.of(trip4, Fare.FareType.orcaRegular, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f),
            Arguments.of(trip4, Fare.FareType.orcaSenior, 0f + 0f + 200f),
            Arguments.of(trip4, Fare.FareType.orcaYouth, 0f + 0f + 300f),
            Arguments.of(trip5, Fare.FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 2),
            Arguments.of(trip5, Fare.FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 2),
            Arguments.of(trip5, Fare.FareType.youth, DEFAULT_RIDE_PRICE_IN_CENTS * 2),
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
            rides.add(tripStrategy.get(rideType));
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
