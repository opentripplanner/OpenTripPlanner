package org.opentripplanner.routing.impl;

import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

public class OrcaFareServiceImpl extends DefaultFareServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(OrcaFareServiceImpl.class);

    public enum RideType {
        COMM_TRANS_LOCAL_SWIFT,
        COMM_TRANS_COMMUTER_EXPRESS,
        EVERETT_TRANSIT,
        KC_WATER_TAXI_VASHON_ISLAND,
        KC_WATER_TAXI_WEST_SEATTLE,
        KC_METRO,
        KITSAP_TRANSIT,
        PIERCE_COUNTY_TRANSIT,
        SEATTLE_STREET_CAR,
        SOUND_TRANSIT,
        WASHINGTON_STATE_FERRIES
    }

    public boolean IS_TEST;
    public static final float DEFAULT_TEST_RIDE_PRICE = 3.49f;

    public OrcaFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(Fare.FareType.regular, regularFareRules);
        addFareRules(Fare.FareType.senior, regularFareRules);
        addFareRules(Fare.FareType.youth, regularFareRules);
        addFareRules(Fare.FareType.orcaRegular, regularFareRules);
        addFareRules(Fare.FareType.orcaYouth, regularFareRules);
        addFareRules(Fare.FareType.orcaLift, regularFareRules);
        addFareRules(Fare.FareType.orcaSenior, regularFareRules);
    }

    private static final long serialVersionUID = 20210625L;

    /**
     * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. In
     * some cases the route description and short name are needed to define inner agency ride types.
     */
    private static RideType classify(Route routeData) {
        if ("29".equals(routeData.getAgency().getId())) {
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
        } else if ("1".equals(routeData.getAgency().getId())) {
            if (routeData.getType() == 4 && routeData.getDesc().contains("Water Taxi: West Seattle")) {
                return RideType.KC_WATER_TAXI_WEST_SEATTLE;
            } else if (routeData.getType() == 4 && routeData.getDesc().contains("Water Taxi: Vashon Island")) {
                return RideType.KC_WATER_TAXI_VASHON_ISLAND;
            }
            return RideType.KC_METRO;
        } else if ("40".equals(routeData.getAgency().getId())) {
            return RideType.SOUND_TRANSIT;
        } else if ("97".equals(routeData.getAgency().getId())) {
            return RideType.EVERETT_TRANSIT;
        } else if ("3".equals(routeData.getAgency().getId())) {
            return RideType.PIERCE_COUNTY_TRANSIT;
        } else if ("23".equals(routeData.getAgency().getId())) {
            return RideType.SEATTLE_STREET_CAR;
        } else if ("wsf".equalsIgnoreCase(routeData.getAgency().getId())) {
            return RideType.WASHINGTON_STATE_FERRIES;
        } else if ("kt".equalsIgnoreCase(routeData.getAgency().getId())) {
            return RideType.KITSAP_TRANSIT;
        }
        return null;
    }

    private float getDiscountedFare(Fare.FareType fareType, RideType rideType, float defaultFare) {
        if (rideType == null) {
            return defaultFare;
        }
        switch (fareType) {
            case youth:
            case orcaYouth:
                return getYouthFare(rideType);
            case orcaLift:
                return getLiftFare(rideType, defaultFare);
            case orcaSenior:
            case senior:
                return getSeniorFare(fareType, rideType, defaultFare);
            case orcaRegular:
                return getOrcaRegularFare(rideType, defaultFare);
            case regular:
            default:
                return defaultFare;
        }
    }

    private float getOrcaRegularFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case KC_WATER_TAXI_VASHON_ISLAND: return 5.75f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 5.00f;
            default: return defaultFare;
        }
    }

    private float getLiftFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case PIERCE_COUNTY_TRANSIT: return defaultFare;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KITSAP_TRANSIT: return 1.00f;
            // KCM, Sound Transit, Everett, Seattle Streetcar.
            default: return 1.50f;
        }
    }

    private float getSeniorFare(Fare.FareType fareType, RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case EVERETT_TRANSIT:
                return fareType.equals(Fare.FareType.orcaSenior) ? 0.50f : defaultFare;
            case PIERCE_COUNTY_TRANSIT:
            case SEATTLE_STREET_CAR:
            case KITSAP_TRANSIT:
                // Pierce, Seattle Streetcar, and Kitsap only provide discounted senior fare for orca.
                return fareType.equals(Fare.FareType.orcaSenior) ? 1.00f : defaultFare;
            case KC_WATER_TAXI_VASHON_ISLAND: return 3.00f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 2.50f;
            // KC Metro, Sound Transit
            default: return 1.00f;
        }
    }

    private float getYouthFare(RideType rideType) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.75f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 3.00f;
            case PIERCE_COUNTY_TRANSIT: return 1.00f;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case KITSAP_TRANSIT: return 2.00f;
            // Default case accounts for KC_METRO, Sound Transit, Everett, and Seattle Streetcar.
            default: return 1.50f;
        }
    }

    private float getRidePrice(Ride ride, Fare.FareType fareType, Collection<FareRuleSet> fareRules) {
        if (IS_TEST) {
            // Testing, return default test ride price.
            return DEFAULT_TEST_RIDE_PRICE;
        }
        List<Ride> ridesSingleton = new ArrayList<>();
        ridesSingleton.add(ride);
        return calculateCost(fareType, ridesSingleton, fareRules);
    }

    @Override
    protected boolean populateFare(Fare fare,
                                   Currency currency,
                                   Fare.FareType fareType,
                                   List<Ride> rides,
                                   Collection<FareRuleSet> fareRules
    ) {
        return calculateFare(fare, currency, fareType, rides, fareRules);
    }

    /**
     * Public access to allow unit testing.
     */
    public boolean calculateFare(Fare fare,
                                 Currency currency,
                                 Fare.FareType fareType,
                                 List<Ride> rides,
                                 Collection<FareRuleSet> fareRules
    ) {
        float cost = 0;
        for (Ride ride : rides) {
            RideType rideType = classify(ride.routeData);
            float singleLegPrice = getRidePrice(ride, fareType, fareRules);
            float discountedFare = getDiscountedFare(fareType, rideType, singleLegPrice);
            if (hasFreeTransfers(fareType, rideType)) {
                // If using Orca (free transfers), the total fare should be equivalent to the
                // most expensive leg of the journey.
                cost = Float.max(cost, discountedFare);
            } else {
                // If free transfers not permitted (i.e., paying with cash), accumulate each
                // leg as we go.
                cost += singleLegPrice;
            }
        }
        if (cost < Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(currency, cost));
        }
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }

    private boolean hasFreeTransfers(Fare.FareType fareType, RideType rideType) {
        return rideType != RideType.WASHINGTON_STATE_FERRIES && usesOrca(fareType);
    }

    private boolean usesOrca(Fare.FareType fareType) {
        return fareType.equals(Fare.FareType.orcaLift) ||
            fareType.equals(Fare.FareType.orcaSenior) ||
            fareType.equals(Fare.FareType.orcaRegular) ||
            fareType.equals(Fare.FareType.orcaYouth);
    }
}
