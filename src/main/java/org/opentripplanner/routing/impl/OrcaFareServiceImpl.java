package org.opentripplanner.routing.impl;

import org.opentripplanner.model.Route;
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
        SOUTH_TRANSIT,
        WASHINGTON_STATE_FERRIES
    }

    // Testing only.
    public OrcaFareServiceImpl() {}


    public OrcaFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(Fare.FareType.regular, regularFareRules);
        addFareRules(Fare.FareType.youth, regularFareRules);
        addFareRules(Fare.FareType.orcaRegular, regularFareRules);
        addFareRules(Fare.FareType.orcaYouth, regularFareRules);
        addFareRules(Fare.FareType.orcaLift, regularFareRules);
        addFareRules(Fare.FareType.orcaReduced, regularFareRules);
    }

    private static final long serialVersionUID = 20210625L;

    /**
     * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. Above
     * this the route type and description is checked.
     */
    private static RideType classify(Route route) {
        if (route == null) {
            return null;
        }
        String agency = route.getAgency().getName();
        if ("Community Transit".equals(agency)) {
            if (route.getType() == 3) {
                return RideType.COMM_TRANS_LOCAL_SWIFT;
            } else {
                // FIXME: The only route type used is 3 (Bus) so assuming for now that anything else is comm express.
                return RideType.COMM_TRANS_COMMUTER_EXPRESS;
            }
        } else if ("Metro Transit".equals(agency)) {
            if (route.getType() == 4 && route.getDesc().contains("Water Taxi: West Seattle")) {
                return RideType.KC_WATER_TAXI_WEST_SEATTLE;
            } else if (route.getType() == 4 && route.getDesc().contains("Water Taxi: Vashon Island")) {
                return RideType.KC_WATER_TAXI_VASHON_ISLAND;
            }
            return RideType.KC_METRO;
        } else if ("Sound Transit".equals(agency)) {
            return RideType.SOUTH_TRANSIT;
        } else if ("Everett Transit".equals(agency)) {
            return RideType.EVERETT_TRANSIT;
        } else if ("Pierce Transit".equals(agency)) {
            return RideType.PIERCE_COUNTY_TRANSIT;
        } else if ("City of Seattle".equals(agency)) {
            return RideType.SEATTLE_STREET_CAR;
        } else if ("Washington State Ferries".equals(agency)) {
            return RideType.WASHINGTON_STATE_FERRIES;
        } else if ("Kitsap Transit".equals(agency)) {
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
    private Float getOrcaFare(float regularCashFare, float youthCashFare, Fare.FareType fareType, RideType rideType) {

        if (rideType == null) {
            return null;
        }

        // Apart from KC water taxi and WSF, orca regular and orca youth match their cash equivalents.
        switch (fareType) {
            case orcaRegular:
                if (rideType == RideType.KC_WATER_TAXI_VASHON_ISLAND) {
                    return 5.75f;
                } else if (rideType == RideType.KC_WATER_TAXI_WEST_SEATTLE) {
                    return 5f;
                } else if (rideType == RideType.WASHINGTON_STATE_FERRIES) {
                    return null;
                }
                return regularCashFare;
            case orcaYouth:
                if (rideType == RideType.KC_WATER_TAXI_VASHON_ISLAND) {
                    return 4.5f;
                } else if (rideType == RideType.KC_WATER_TAXI_WEST_SEATTLE) {
                    return 3.75f;
                } else if (rideType == RideType.WASHINGTON_STATE_FERRIES) {
                    return null;
                }
                return youthCashFare;
        }

        switch (rideType) {
            case COMM_TRANS_LOCAL_SWIFT:
            case COMM_TRANS_COMMUTER_EXPRESS:
                switch (fareType) {
                    case orcaLift:
                    case orcaReduced:
                        if (rideType == RideType.COMM_TRANS_LOCAL_SWIFT) {
                            return 1.25f;
                        } else {
                            return 2f;
                        }
                }
                break;
            case EVERETT_TRANSIT:
                switch (fareType) {
                    case orcaLift:
                        return youthCashFare;
                    case orcaReduced:
                        return 0.5f;
                }
                break;
            case KC_METRO:
            case SOUTH_TRANSIT:
                switch (fareType) {
                    case orcaLift:
                        return youthCashFare;
                    case orcaReduced:
                        return 1f;
                }
                break;
            case PIERCE_COUNTY_TRANSIT:
                switch (fareType) {
                    case orcaLift:
                        return null;
                    case orcaReduced:
                        return 1f;
                }
                break;
            case KC_WATER_TAXI_VASHON_ISLAND:
                switch (fareType) {
                    case orcaLift:
                        return youthCashFare;
                    case orcaReduced:
                        return 3f;
                }
                break;
            case KC_WATER_TAXI_WEST_SEATTLE:
                switch (fareType) {
                    case orcaLift:
                        return youthCashFare;
                    case orcaReduced:
                        return 2.5f;
                }
                break;
            case WASHINGTON_STATE_FERRIES:
                switch (fareType) {
                    case orcaLift:
                    case orcaReduced:
                        return null;
                }
                break;
            case SEATTLE_STREET_CAR:
                switch (fareType) {
                    case orcaLift:
                        return youthCashFare;
                    case orcaReduced:
                        return 1f;
                }
                break;
            case KITSAP_TRANSIT:
                switch (fareType) {
                    case orcaLift:
                    case orcaReduced:
                        return 1f;
                }
                break;
        }
        return regularCashFare;
    }

    //TODO: merge with populateFare once Route issue has been resolved.
    public Float calcFare(Route route,
                          float regularCashFare,
                          float youthCashFare,
                          Fare.FareType fareType
    ) {
        RideType rideType = classify(route);
        return getOrcaFare(regularCashFare, youthCashFare, fareType, rideType);
    }

    @Override
    protected boolean populateFare(Fare fare,
                                   Currency currency,
                                   Fare.FareType fareType,
                                   List<Ride> rides,
                                   Collection<FareRuleSet> fareRules
    ) {
        float cost = 0;
        float WSFCost = 0;
        if (
            fareType == Fare.FareType.orcaRegular ||
            fareType == Fare.FareType.orcaYouth ||
            fareType == Fare.FareType.orcaLift ||
            fareType == Fare.FareType.orcaReduced
        ) {
            for (Ride ride : rides) {
                List<Ride> leg = new ArrayList<>();
                leg.add(ride);
                //TODO: get Route obj from Ride obj, DB call?
                Route route = null;
                float regularCashFare = calculateCost(Fare.FareType.regular, leg, fareRules);
                float youthCashFare = calculateCost(Fare.FareType.youth, leg, fareRules);
                RideType rideType = classify(route);
                Float legCost = calcFare(null, regularCashFare, youthCashFare, fareType);
                if (legCost == null) {
                    // FIXME: Orca not valid for this ride type. Apply regular cash value.
                    legCost = regularCashFare;
                }
                if (rideType == RideType.WASHINGTON_STATE_FERRIES) {
                    //WSF do not except transfers.
                    WSFCost += legCost;
                } else {
                    cost = Float.max(cost, legCost);
                }
            }
            cost += WSFCost;
        } else {
            // Get cash fare for complete journey.
            cost = calculateCost(fareType, rides, fareRules);
        }
        if (cost < Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(currency, cost));
        }
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }
}
