package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import com.google.common.collect.Iterables;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdxFareServiceImpl extends DefaultFareServiceImpl {

    public PdxFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(FareType.regular, regularFareRules);
        addFareRules(FareType.senior, regularFareRules);
        addFareRules(FareType.special, regularFareRules);
        addFareRules(FareType.youth, regularFareRules);
    }

    private static final long serialVersionUID = 20120229L;
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(PdxFareServiceImpl.class);

    /**
     * The fare calculation for the Portland area is relatively simple. The Hop pass is designed to charge
     * the rider incrementally as they use each service. The total cost of the trip will be equal to the leg of the
     * journey that had the maximum fare. This fare structure applies to TriMet, Portland Streetcar, and C-TRAN.
     * More info on the inter-agency fare structure is here: https://trimet.org/hop/fares.htm
     */
    @Override
    protected boolean populateFare(Fare fare, Currency currency, FareType fareType, List<Ride> rides,
                                   Collection<FareRuleSet> fareRules) {
        fare.addFare(FareType.regular, new WrappedCurrency(currency),0);
        fare.addFare(FareType.senior, new WrappedCurrency(currency),0);
        fare.addFare(FareType.special, new WrappedCurrency(currency),0);
        fare.addFare(FareType.youth, new WrappedCurrency(currency),0);
        float lowestCost = getLowestCost(fareType, rides, fareRules);
        float cost = 0;
        for (Ride ride : rides) {
            List<Ride> leg = new ArrayList<>();
            leg.add(ride);
            float rideCost = calculateCost(fareType, leg, fareRules);
            cost = Float.max(cost, rideCost);
        }
        if (cost < Float.POSITIVE_INFINITY) fare.addFare(fareType, getMoney(currency, cost));
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }

}
