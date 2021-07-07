package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

public class OrcaFareServiceImpl extends DefaultFareServiceImpl {

    enum RideType {
        COMM_TRANS_LOCAL_SWIFT,
        COMM_TRANS_COMMUTER_EXPRESS,
        EVERETT_TRANSIT,
        INTERCITY_TRANSIT,
        KC_WATER_TAXI_VASHON_ISLAND,
        KC_WATER_TAXI_WEST_SEATTLE,
        KC_METRO,
        KITSAP_TRANSIT,
        PIERCE_COUNTY_TRANSIT,
        SEATTLE_STREET_CAR,
        SOUND_TRANSIT,
        WASHINGTON_STATE_FERRIES
    }

    // Testing only.
    public OrcaFareServiceImpl() {}


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
     * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. Above
     * this the route type and description is checked.
     */
    private static RideType classify(String agencyId, String routeId) {
        if ("Community Transit".equals(agencyId)) {
            if (route.getType() == 3) {
                return RideType.COMM_TRANS_LOCAL_SWIFT;
            } else {
                // FIXME: The only route type used is 3 (Bus) so assuming for now that anything else is comm express.
                return RideType.COMM_TRANS_COMMUTER_EXPRESS;
            }
        } else if ("Metro Transit".equals(agencyId)) {
            if (route.getType() == 4 && route.getDesc().contains("Water Taxi: West Seattle")) {
                return RideType.KC_WATER_TAXI_WEST_SEATTLE;
            } else if (route.getType() == 4 && route.getDesc().contains("Water Taxi: Vashon Island")) {
                return RideType.KC_WATER_TAXI_VASHON_ISLAND;
            }
            return RideType.KC_METRO;
        } else if ("Sound Transit".equals(agencyId)) {
            return RideType.SOUND_TRANSIT;
        } else if ("Everett Transit".equals(agencyId)) {
            return RideType.EVERETT_TRANSIT;
        } else if ("Pierce Transit".equals(agencyId)) {
            return RideType.PIERCE_COUNTY_TRANSIT;
        } else if ("City of Seattle".equals(agencyId)) {
            return RideType.SEATTLE_STREET_CAR;
        } else if ("Washington State Ferries".equals(agencyId)) {
            return RideType.WASHINGTON_STATE_FERRIES;
        } else if ("Kitsap Transit".equals(agencyId)) {
            return RideType.KITSAP_TRANSIT;
        }
        return null;
    }

    /**
     * Return the Orca discount if applicable. In most cases the orca fare for regular and youth is the same as the
     * cash fare with the following exceptions:
     * 1) KC water taxi where the orca regular and youth fares are less.
     * 2) WSF where Orca can not be used.
     * 3) Pierce County where orca Lift can not be used.
     *
     * In cases where Orca is not applicable a null value is returned. No assumptions are therefore made regarding the
     * correct cash value to be applied.
     */
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
            case regular:
            default:
                return defaultFare;
        }
    }

    private float getLiftFare(RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case PIERCE_COUNTY_TRANSIT: return defaultFare;
            case KC_WATER_TAXI_VASHON_ISLAND: return 4.50f;
            case KC_WATER_TAXI_WEST_SEATTLE: return 3.75f;
            case INTERCITY_TRANSIT: return 0.00f;
            case KITSAP_TRANSIT: return 1.00f;
            // KCM, Sound Transit, Everett, Seattle Streetcar.
            default: return 1.50f;
        }
    }

    private float getSeniorFare(Fare.FareType fareType, RideType rideType, float defaultFare) {
        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT: return 1.25f;
            case COMM_TRANS_COMMUTER_EXPRESS: return 2.00f;
            case INTERCITY_TRANSIT: return 0.00f;
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
            case INTERCITY_TRANSIT: return 0.00f;
            // Default case accounts for KC_METRO, Sound Transit, Everett, and Seattle Streetcar.
            default: return 1.50f;
        }
    }

    private float getRidePrice(Ride ride, Fare.FareType fareType, Collection<FareRuleSet> fareRules) {
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
        float cost = 0;
        for (Ride ride : rides) {
            String routeId = ride.route.getId();
            String agencyId = ride.agency;
            RideType rideType = classify(agencyId, routeId);
            float singleLegPrice = getRidePrice(ride, Fare.FareType.regular, fareRules);
            float discountedFare = getDiscountedFare(fareType, rideType, singleLegPrice);
            if (hasFreeTransfers(fareType, rideType)) {
                // If using Orca (free transfers), the total fare should be equivalent to the
                // most expensive leg of the journey.
                cost = Float.max(cost, discountedFare);
            } else {
                // If free transfers not permitted (i.e., paying with cash), accumulate each
                // leg as we go.
                cost += discountedFare;
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
