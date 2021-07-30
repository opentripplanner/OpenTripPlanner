package org.opentripplanner.routing.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Calculate Orca discount fares based on the fare type and the agencies traversed within a trip.
 */
public class OrcaFareServiceImpl extends DefaultFareServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(OrcaFareServiceImpl.class);

    private static final long FREE_TRANSFER_TIME_DURATION = 7200; // 2 hours

    public static final String COMM_TRANS_AGENCY_ID = "29";
    public static final String KC_METRO_AGENCY_ID = "1";
    public static final String SOUND_TRANSIT_AGENCY_ID = "40";
    public static final String EVERETT_TRANSIT_AGENCY_ID = "97";
    public static final String PIERCE_COUNTY_TRANSIT_AGENCY_ID = "3";
    public static final String SKAGIT_TRANSIT_AGENCY_ID = "e0e4541a-2714-487b-b30c-f5c6cb4a310f";
    public static final String SEATTLE_STREET_CAR_AGENCY_ID = "23";
    public static final String WASHINGTON_STATE_FERRIES_AGENCY_ID = "wsf";
    public static final String KITSAP_TRANSIT_AGENCY_ID = "kt";
    public static final int ROUTE_TYPE_FERRY = 4;

    public enum RideType {
        COMM_TRANS_LOCAL_SWIFT,
        COMM_TRANS_COMMUTER_EXPRESS,
        EVERETT_TRANSIT,
        KC_WATER_TAXI_VASHON_ISLAND,
        KC_WATER_TAXI_WEST_SEATTLE,
        KC_METRO,
        KITSAP_TRANSIT,
        KITSAP_TRANSIT_FAST_FERRY_EASTBOUND,
        KITSAP_TRANSIT_FAST_FERRY_WESTBOUND,
        PIERCE_COUNTY_TRANSIT,
        SKAGIT_TRANSIT,
        SEATTLE_STREET_CAR,
        SOUND_TRANSIT,
        WASHINGTON_STATE_FERRIES
    }

    private static final Map<String, Function<Route, RideType>> classificationStrategy = new HashMap<>();
    private static final Map<String, Map<Fare.FareType, Float>> washingtonStateFerriesFares = new HashMap<>();

    // If set to true, the test ride price is used instead of the actual agency cash fare.
    public boolean IS_TEST;
    public static final float DEFAULT_TEST_RIDE_PRICE = 3.49f;

    public OrcaFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(Fare.FareType.regular, regularFareRules);
        addFareRules(Fare.FareType.senior, regularFareRules);
        addFareRules(Fare.FareType.youth, regularFareRules);
        addFareRules(Fare.FareType.electronicRegular, regularFareRules);
        addFareRules(Fare.FareType.electronicYouth, regularFareRules);
        addFareRules(Fare.FareType.electronicSpecial, regularFareRules);
        addFareRules(Fare.FareType.electronicSenior, regularFareRules);

        classificationStrategy.put(
            COMM_TRANS_AGENCY_ID,
            routeData -> {
                try {
                    int routeId = Integer.parseInt(routeData.getShortName());
                    if (routeId >= 400 && routeId <= 899) {
                        return RideType.COMM_TRANS_COMMUTER_EXPRESS;
                    }
                    return RideType.COMM_TRANS_LOCAL_SWIFT;
                } catch (NumberFormatException e) {
                    LOG.warn("Unable to determine comm trans route id from {}.", routeData.getShortName(), e);
                    return RideType.COMM_TRANS_LOCAL_SWIFT;
                }
            }
        );
        classificationStrategy.put(
            KC_METRO_AGENCY_ID,
            routeData -> {
                if (routeData.getType() == ROUTE_TYPE_FERRY &&
                    routeData.getLongName().contains("Water Taxi: West Seattle")) {
                    return RideType.KC_WATER_TAXI_WEST_SEATTLE;
                } else if (routeData.getType() == ROUTE_TYPE_FERRY &&
                    routeData.getDesc().contains("Water Taxi: Vashon Island")) {
                    return RideType.KC_WATER_TAXI_VASHON_ISLAND;
                }
                return RideType.KC_METRO;
            }
        );
        classificationStrategy.put(SOUND_TRANSIT_AGENCY_ID, routeData -> RideType.SOUND_TRANSIT);
        classificationStrategy.put(EVERETT_TRANSIT_AGENCY_ID, routeData -> RideType.EVERETT_TRANSIT);
        classificationStrategy.put(PIERCE_COUNTY_TRANSIT_AGENCY_ID, routeData -> RideType.PIERCE_COUNTY_TRANSIT);
        classificationStrategy.put(SKAGIT_TRANSIT_AGENCY_ID, routeData -> RideType.SKAGIT_TRANSIT);
        classificationStrategy.put(SEATTLE_STREET_CAR_AGENCY_ID, routeData -> RideType.SEATTLE_STREET_CAR);
        classificationStrategy.put(WASHINGTON_STATE_FERRIES_AGENCY_ID, routeData -> RideType.WASHINGTON_STATE_FERRIES);
        classificationStrategy.put(KITSAP_TRANSIT_AGENCY_ID, routeData -> RideType.KITSAP_TRANSIT);

        // Spaces have been removed from the route name because of inconsistencies in the WSF GTFS route dataset.
        washingtonStateFerriesFares.put(
            "Seattle-BainbridgeIsland",
            ImmutableMap.of(Fare.FareType.regular, 9.05f, Fare.FareType.youth, 4.50f, Fare.FareType.senior, 4.50f)
        );
        washingtonStateFerriesFares.put(
            "Seattle-Bremerton",
            ImmutableMap.of(Fare.FareType.regular, 9.05f, Fare.FareType.youth, 4.50f, Fare.FareType.senior, 4.50f)
        );
        washingtonStateFerriesFares.put(
            "Mukilteo-Clinton",
            ImmutableMap.of(Fare.FareType.regular, 5.55f, Fare.FareType.youth, 2.75f, Fare.FareType.senior, 2.75f)
        );
        washingtonStateFerriesFares.put(
            "Fauntleroy-VashonIsland",
            ImmutableMap.of(Fare.FareType.regular, 5.95f, Fare.FareType.youth, 2.95f, Fare.FareType.senior, 2.95f)
        );
        washingtonStateFerriesFares.put(
            "Fauntleroy-Southworth",
            ImmutableMap.of(Fare.FareType.regular, 7.10f, Fare.FareType.youth, 3.55f, Fare.FareType.senior, 3.55f)
        );
        washingtonStateFerriesFares.put(
            "Edmonds-Kingston",
            ImmutableMap.of(Fare.FareType.regular, 9.05f, Fare.FareType.youth, 4.50f, Fare.FareType.senior, 4.50f)
        );
        washingtonStateFerriesFares.put(
            "PointDefiance-Tahlequah",
            ImmutableMap.of(Fare.FareType.regular, 5.95f, Fare.FareType.youth, 2.95f, Fare.FareType.senior, 2.95f)
        );
        washingtonStateFerriesFares.put(
            "Anacortes-FridayHarbor",
            ImmutableMap.of(Fare.FareType.regular, 14.50f, Fare.FareType.youth, 7.25f, Fare.FareType.senior, 7.25f)
        );
        washingtonStateFerriesFares.put(
            "Anacortes-LopezIsland",
            ImmutableMap.of(Fare.FareType.regular, 14.50f, Fare.FareType.youth, 7.25f, Fare.FareType.senior, 7.25f)
        );
        washingtonStateFerriesFares.put(
            "Anacortes-OrcasIsland",
            ImmutableMap.of(Fare.FareType.regular, 14.50f, Fare.FareType.youth, 7.25f, Fare.FareType.senior, 7.25f)
        );
        washingtonStateFerriesFares.put(
            "Anacortes-ShawIsland",
            ImmutableMap.of(Fare.FareType.regular, 14.50f, Fare.FareType.youth, 7.25f, Fare.FareType.senior, 7.25f)
        );
        washingtonStateFerriesFares.put(
            "Coupeville-PortTownsend",
            ImmutableMap.of(Fare.FareType.regular, 3.80f, Fare.FareType.youth, 1.80f, Fare.FareType.senior, 1.80f)
        );
        washingtonStateFerriesFares.put(
            "PortTownsend-Coupeville",
            ImmutableMap.of(Fare.FareType.regular, 3.80f, Fare.FareType.youth, 1.80f, Fare.FareType.senior, 1.80f)
        );
        washingtonStateFerriesFares.put(
            "Southworth-VashonIsland",
            ImmutableMap.of(Fare.FareType.regular, 5.95f, Fare.FareType.youth, 2.95f, Fare.FareType.senior, 2.95f)
        );
    }

    /**
     * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. In
     * some cases the route description and short name are needed to define inner agency ride types. For Kitsap, the
     * route data is enough to define the agency, but addition trip id checks are needed to define the fast ferry direction.
     */
    private static RideType classify(Route routeData, String tripId) {
        Function<Route, RideType> classifier = classificationStrategy.get(routeData.getAgency().getId());
        if (classifier == null) {
            return null;
        }

        RideType rideType = classifier.apply(routeData);
        if (rideType == RideType.KITSAP_TRANSIT &&
            routeData.getId().getId().equalsIgnoreCase("Kitsap Fast Ferry") &&
            routeData.getType() == ROUTE_TYPE_FERRY
        ) {
            // Additional trip id checks are required to distinguish Kitsap fast ferry routes.
            if (tripId.contains("east")) {
                rideType = RideType.KITSAP_TRANSIT_FAST_FERRY_EASTBOUND;
            } else if (tripId.contains("west")) {
                rideType = RideType.KITSAP_TRANSIT_FAST_FERRY_WESTBOUND;
            }
        }
        return rideType;
    }

    /**
     * Define which discount fare should be applied based on the fare type. If the ride type is unknown the discount
     * fare can not be applied, use the default fare.
     */
    private float getLegFare(Fare.FareType fareType, RideType rideType, float defaultFare, Route route) {
        if (rideType == null) {
            return defaultFare;
        }
        switch (fareType) {
            case youth:
            case electronicYouth:
                return getYouthFare(fareType, rideType, defaultFare, route);
            case electronicSpecial:
                return getLiftFare(rideType, defaultFare);
            case electronicSenior:
            case senior:
                return getSeniorFare(fareType, rideType, defaultFare, route);
            case regular:
            case electronicRegular:
                return getRegularFare(fareType, rideType, defaultFare, route);
            default:
                return defaultFare;
        }
    }

    /**
     * Apply regular discount fares. If the ride type cannot be matched the default fare is used.
     */
    private float getRegularFare(Fare.FareType fareType, RideType rideType, float defaultFare, Route route) {
        switch (rideType) {
            case KC_WATER_TAXI_VASHON_ISLAND: return 5.75f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 5.00f;
            case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND: return 2.00f;
            case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND: return 10.00f;
            case WASHINGTON_STATE_FERRIES:
                return getWashingtonStateFerriesFare(route.getLongName(), fareType, defaultFare);
            default: return defaultFare;
        }
    }

    /**
     * Apply Orca lift discount fares based on the ride type.
     */
    private float getLiftFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KITSAP_TRANSIT: return 1.00f;
            case KC_METRO:
            case SOUND_TRANSIT:
            case EVERETT_TRANSIT:
            case SEATTLE_STREET_CAR:
                return 1.50f;
            case PIERCE_COUNTY_TRANSIT:
            default:
                return defaultFare;
        }
    }

    /**
     * Apply senior discount fares based on the fare and ride types.
     */
    private float getSeniorFare(Fare.FareType fareType, RideType rideType, float defaultFare, Route route) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case EVERETT_TRANSIT:
                return fareType.equals(Fare.FareType.electronicSenior) ? 0.50f : defaultFare;
            case PIERCE_COUNTY_TRANSIT:
            case SEATTLE_STREET_CAR:
            case KITSAP_TRANSIT:
                // Pierce, Seattle Streetcar, and Kitsap only provide discounted senior fare for orca.
                return fareType.equals(Fare.FareType.electronicSenior) ? 1.00f : defaultFare;
            case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND:
                // Kitsap only provide discounted senior fare for orca.
                return fareType.equals(Fare.FareType.electronicSenior) ? 1.00f : 2.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 3.00f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 2.50f;
            case KC_METRO:
            case SOUND_TRANSIT:
                return 1.00f;
            case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND:
                return fareType.equals(Fare.FareType.electronicSenior) ? 5.00f : 10.00f;
            case SKAGIT_TRANSIT:
                // Discount specific to Skagit transit and not Orca.
                return 0.50f;
            case WASHINGTON_STATE_FERRIES:
                return getWashingtonStateFerriesFare(route.getLongName(), fareType, defaultFare);
            default: return defaultFare;
        }
    }

    /**
     * Apply youth discount fares based on the ride type.
     */
    private float getYouthFare(Fare.FareType fareType, RideType rideType, float defaultFare, Route route) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.75f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 3.00f;
            case KITSAP_TRANSIT:
            case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND:
                // Discount specific to Kitsap transit.
                return fareType.equals(Fare.FareType.electronicYouth) ? 1.00f : 2.00f;
            case PIERCE_COUNTY_TRANSIT: return 1.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KC_METRO:
            case SOUND_TRANSIT:
            case EVERETT_TRANSIT:
            case SEATTLE_STREET_CAR: return 1.50f;
            case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND:
                // Discount specific to Kitsap transit.
                return fareType.equals(Fare.FareType.electronicYouth) ? 5.00f : 10.00f;
            case SKAGIT_TRANSIT:
                // Discount specific to Skagit transit.
                return 0.50f;
            case WASHINGTON_STATE_FERRIES:
                // Discount specific to WSF.
                return getWashingtonStateFerriesFare(route.getLongName(), fareType, defaultFare);
            default: return defaultFare;
        }
    }

    /**
     * Get the washington state ferries fare matching the route long name and fare type. If no match is found, return
     * the default fare.
     */
    private float getWashingtonStateFerriesFare(String routeLongName, Fare.FareType fareType, float defaultFare) {
        if (routeLongName == null || routeLongName.isEmpty()) {
            return defaultFare;
        }
        Float fare = washingtonStateFerriesFares.get(routeLongName.replaceAll(" ", "")).get(fareType);
        return (fare != null) ? fare : defaultFare;
    }

    /**
     * Get the ride price for a single leg. If testing, this class is being called directly so the required agency cash
     * values are not available therefore the default test price is used instead.
     */
    private float getRidePrice(Ride ride, Fare.FareType fareType, Collection<FareRuleSet> fareRules) {
        if (IS_TEST) {
            // Testing, return default test ride price.
            return DEFAULT_TEST_RIDE_PRICE;
        }
        return calculateCost(fareType, Lists.newArrayList(ride), fareRules);
    }

    /**
     * Calculate the cost of a journey. Where free transfers are not permitted the cash price is used. If free transfers
     * are applicable, the most expensive discount fare across all legs is added to the final cumulative price.
     *
     * The computed fare for Orca card users takes into account realtime trip updates where available, so that, for
     * instance, when a leg on a long itinerary is delayed to begin after the initial two hour window has expired,
     * the calculated fare for that trip will be two one-way fares instead of one.
     */
    @Override
    public boolean populateFare(Fare fare,
                                   Currency currency,
                                   Fare.FareType fareType,
                                   List<Ride> rides,
                                   Collection<FareRuleSet> fareRules
    ) {
        Long freeTransferStartTime = null;
        float cost = 0;
        float orcaFareDiscount = 0;
        for (Ride ride : rides) {
            RideType rideType = classify(ride.routeData, ride.trip.getId());
            boolean ridePermitsFreeTransfers = permitsFreeTransfers(rideType);
            if (freeTransferStartTime == null && ridePermitsFreeTransfers) {
                // The start of a free transfer must be with a transit agency that permits it!
                freeTransferStartTime = ride.startTime;
            }
            float singleLegPrice = getRidePrice(ride, fareType, fareRules);
            float legFare = getLegFare(fareType, rideType, singleLegPrice, ride.routeData);
            boolean inFreeTransferWindow = inFreeTransferWindow(freeTransferStartTime, ride.startTime);
            if (hasFreeTransfers(fareType, rideType) && inFreeTransferWindow) {
                // If using Orca (free transfers), the total fare should be equivalent to the
                // most expensive leg of the journey.
                orcaFareDiscount = Float.max(orcaFareDiscount, legFare);
           } else if (usesOrca(fareType) && !inFreeTransferWindow) {
                // If using Orca and outside of the free transfer window, add the cumulative Orca fare (the maximum leg 
                // fare encountered within the free transfer window).
                cost += orcaFareDiscount;
                
                // Reset the free transfer start time and next Orca fare as needed.
                if (ridePermitsFreeTransfers) {
                    // The leg is using a ride type that permits free transfers. 
                    // The next free transfer window begins at the start time of this leg.
                    freeTransferStartTime = ride.startTime;
                    // Reset the Orca fare to be the fare of this leg.
                    orcaFareDiscount = legFare;
                } else {
                    // The leg is not using a ride type that permits free transfers.
                    // Since there are no free transfers for this leg, increase the total cost by the fare for this leg.
                    cost += legFare;
                    // The current free transfer window has expired and won't start again until another leg is 
                    // encountered that does have free transfers.
                    freeTransferStartTime = null;
                    // The previous Orca fare has been applied to the total cost. Also, the non-free transfer cost has
                    // also been applied to the total cost. Therefore, the next Orca cost for the next free-transfer 
                    // window needs to be reset to 0 so that it is not applied after looping through all rides.
                    orcaFareDiscount = 0;
                }
            } else {
                // If not using Orca, add the agencies default price for this leg.
                cost += legFare;
            }
        }
        cost += orcaFareDiscount;
        if (cost < Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(currency, cost));
        }
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }

    /**
     *  Trip within the free two hour transfer window.
     */
    private boolean inFreeTransferWindow(Long freeTransferStartTime, long currentLegStartTime) {
        return freeTransferStartTime != null &&
            currentLegStartTime < freeTransferStartTime + FREE_TRANSFER_TIME_DURATION;
    }

    /**
     * A free transfer can be applied if using Orca and the transit agency permits free transfers.
     */
    private boolean hasFreeTransfers(Fare.FareType fareType, RideType rideType) {
        return permitsFreeTransfers(rideType) && usesOrca(fareType);
    }

    /**
     * All transit agencies permit free transfers, apart from these.
     */
    private boolean permitsFreeTransfers(RideType rideType) {
        return rideType != RideType.WASHINGTON_STATE_FERRIES && rideType != RideType.SKAGIT_TRANSIT;
    }

    /**
     * Define Orca fare types.
     */
    private boolean usesOrca(Fare.FareType fareType) {
        return fareType.equals(Fare.FareType.electronicSpecial) ||
            fareType.equals(Fare.FareType.electronicSenior) ||
            fareType.equals(Fare.FareType.electronicRegular) ||
            fareType.equals(Fare.FareType.electronicYouth);
    }
}
