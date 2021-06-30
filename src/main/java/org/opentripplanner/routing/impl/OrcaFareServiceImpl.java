package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

public class OrcaFareServiceImpl extends DefaultFareServiceImpl {
    private static final String WASHINGTON_STATE_FERRIES_AGENCY = "Washington State Ferries";

//  TODO: Do we need to consider paper transfers? King County Metro and Kitsap Transit offer paper transfers that
//   are good within each system. Community Transit, Everett Transit, Pierce Transit and Sound Transit do not accept paper transfers.

// TODO: Do we need to consider expiration? Transfer value expires two hours after tapping the card. If the initial
//  trip is less than the amount of fare required for a transfer, the difference must be paid with cash or E-purse.

    public OrcaFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(Fare.FareType.regular, regularFareRules);
        addFareRules(Fare.FareType.youth, regularFareRules);
        addFareRules(Fare.FareType.orcaReduced, regularFareRules);
        addFareRules(Fare.FareType.orcaRegular, regularFareRules);
        addFareRules(Fare.FareType.orcaYouth, regularFareRules);
    }

    private static final long serialVersionUID = 20210625L;
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(OrcaFareServiceImpl.class);

    @Override
    protected boolean populateFare(Fare fare,
                                   Currency currency,
                                   Fare.FareType fareType,
                                   List<Ride> rides,
                                   Collection<FareRuleSet> fareRules
    ) {
        // Get full youth fare.
        saveFare(calculateCost(Fare.FareType.youth, rides, fareRules), fare, Fare.FareType.youth, currency);
        // Get full regular fare.
        float regularCost = calculateCost(Fare.FareType.regular, rides, fareRules);
        saveFare(regularCost, fare, Fare.FareType.regular, currency);

        if (regularCost > 0 && regularCost < Float.POSITIVE_INFINITY) {
            float ocraRegularCost = getOrcaRegularFare(regularCost, fareType, rides, fareRules);
            // Apply ORCA discounts to regular cost.
            saveFare(ocraRegularCost, fare, Fare.FareType.orcaRegular, currency);
            // TODO: Reduced and Youth.
        }
        return fare.fare.size() > 0;
    }

    private void saveFare(float cost, Fare fare,Fare.FareType fareType, Currency currency) {
        if (cost < Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(currency, cost));
        }
    }

    private float getOrcaRegularFare(float regularCost,
                                     Fare.FareType fareType,
                                     List<Ride> rides,
                                     Collection<FareRuleSet> fareRules
    ) {
        if (rides.size() == 1 ||
            rides.size() == 2 && rides.get(1).agency.equalsIgnoreCase(WASHINGTON_STATE_FERRIES_AGENCY)
        ) {
            // An orca discount can not be applied to a single leg journey or Washington state ferry transfers.
            return regularCost;
        }

        // Orca regular cost is equal to the cash regular costs minus the cost of the first leg.
        List<Ride> leg = new ArrayList<>();
        Ride firstTransfer = rides.get(0);
        leg.add(firstTransfer);
        return regularCost - calculateCost(fareType, leg, fareRules);
    }
}
