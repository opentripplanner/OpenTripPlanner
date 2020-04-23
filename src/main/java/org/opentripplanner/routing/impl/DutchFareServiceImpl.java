package org.opentripplanner.routing.impl;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Currency;
import java.util.List;

public class DutchFareServiceImpl extends DefaultFareServiceImpl {

    public DutchFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(FareType.regular, regularFareRules);
    }

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(DutchFareServiceImpl.class);

    public static final int TRANSFER_DURATION = 60 * 35; /* tranfers within 35 min won't require a new base fare */

    final Currency euros = Currency.getInstance("EUR");
    
    /**
     * This overridden method completely ignores the Currency object supplied by the caller.
     * This is because the caller in the superclass assumes the input data uses only one currency.
     * However, Dutch data contains fares in both Euros and Dutch Railways fare units, with the added complexity
     * that these pseudo-currency units do not have sub-units in the way Euros have cents, which leads to
     * incorrect rounding and scaling etc.  While the fare rules consulted by this fare service do have a mix of EUR 
     * and train pseudo-units, this Fare object is accumulating the monetary fare returned to the user and is known 
     * to always be in Euros. See issue #2679 for discussion.
     */
    @Override
    protected boolean populateFare(Fare fare, Currency currency, FareType fareType, List<Ride> rides,
                                   Collection<FareRuleSet> fareRules) {
        float lowestCost = getLowestCost(fareType, rides, fareRules);
        if(lowestCost != Float.POSITIVE_INFINITY) {
            fare.addFare(fareType, getMoney(euros, lowestCost));
            return true;
        }
        return false;
    }
 
    /* The Netherlands has an almost uniform system for electronic ticketing using a NFC-card, branded as OV-chipkaart.
     *
     * To travel through all modes in The Netherlands a uses has two products on their card:
     * 1) Easy Trip, to travel with all operators except Dutch Railways
     * 2) Reizen op Saldo, for Dutch Railways which should be explicitly
     *    loaded on a card and requires the user to select a first or second class.
     *
     * Check-in and check-out is done using validators. For our calculation on transerfer time it matters if this
     * validator is inside the vehicle - we have to wait for the validator to arrive - or we can already check in
     * on a validator present on the stop, and wait for the vehicle to arrive.
     *
     * Reizen op Saldo is limited to Dutch Railways, and always allows to validate inside the stations.
     * Additionally in the following cases validators are also on the platform or stations.
     *  - Metro of Amsterdam operated by GVB
     *  - Metro of Rotterdam operated by RET
     *  - Lightrail of Utrecht operated by Qbuzz
     *  - All heavy rail services operated by Arriva, Breng, Connexxion, NS, Syntus and Veolia.
     *
     * Leaving the platform or stations implies that the traveler must check-out. Thus a transfer will play a role.
     *
     * All other modes by these operators have validators inside the vehicle.
     *
     * TODO: It is an optimisation to be able to check-in early. And most likely only be visible by a trip which is
     * artificially created to test for this implementation.
     *
     * Long-Distance-Discount for trains
     * Between train operators in The Netherlands long distance discount applies under the following condition:
     *  - a trip between two train operators takes places within 35 minutes
     *
     * First the route is maximised per operator.
     *
     * The price of the next operator consists of:
     *     globally traveled units = 100 previous operator(s)
     *     locally  traveled units = 10
     *
     *     (DutchRailwaysPrice(0 + 100) - DutchRailwaysPrice(0)) + (ArrivaPrice(100 + 10) - ArrivaPrice(100))
     */

    private class UnitsFareZone {
        public int units;
        public String fareZone;

        public UnitsFareZone(int units, String fareZone) {
            this.units = units;
            this.fareZone = fareZone;
        }
    }

    private UnitsFareZone getUnitsByZones(FeedScopedId agencyId, String startZone, String endZone, Collection<FareRuleSet> fareRules) {
        P2<String> od = new P2<String>(startZone, endZone);

        LOG.trace("Search " + startZone + " and " + endZone);

        String fareIdStartsWith = agencyId.getId() + "::";

        for (FareRuleSet ruleSet : fareRules) {
            if (ruleSet.getFareAttribute().getId().getId().startsWith(fareIdStartsWith) &&
                ruleSet.getOriginDestinations().contains(od)) {
                String fareId = ruleSet.getFareAttribute().getId().getId();
                String[] parts = fareId.split("::");
                String fareZone = parts[1];

                LOG.trace("Between " + startZone + " and " + endZone + ": " + (int) ruleSet.getFareAttribute().getPrice() + " (" + fareZone + ")");
                return new UnitsFareZone((int) ruleSet.getFareAttribute().getPrice(), fareZone);
            }
        }

        LOG.warn("Can't find units between " + startZone + " and " + endZone);

        /* TODO: Raise Exception */

        return null;
    }
    
    private float getCostByUnits(String fareZone, int units, int prevSumUnits, Collection<FareRuleSet> fareRules) {
        if (units == 0) {
            return 0f;
        }

        /* Train-units cannot exceed 250 units; http://wiki.ovinnederland.nl/wiki/Tariefeenheid#Tarieven_NS */
        if (units > 250) {
            units = 250;
        }

        float cost = 0f;

        String fareId = fareZone + ":" + (units + prevSumUnits);
        for (FareRuleSet ruleSet : fareRules) {
            if (ruleSet.getFareAttribute().getId().getId().equals(fareId)) {
                cost = ruleSet.getFareAttribute().getPrice();
                break;
            }
        }

        if (cost == 0f) {
            LOG.warn("Can't find price for " + fareZone + " with " + units + " units");

        } else if (prevSumUnits > 0) {

            fareId = fareZone + ":" + prevSumUnits;
            for (FareRuleSet ruleSet : fareRules) {
                if (ruleSet.getFareAttribute().getId().getId().equals(fareId)) {
                    cost -= ruleSet.getFareAttribute().getPrice();
                    return cost;
                }
            }

            LOG.warn("Can't find price for " + fareZone + " with " + prevSumUnits + " units");

            return Float.POSITIVE_INFINITY;
        }

        return cost;
    }

    private float getEasyTripFareByLineFromTo(String route, String firstStop, String lastStop,
                                              boolean entranceFee, Collection<FareRuleSet> fareRules) {

        float cost = Float.POSITIVE_INFINITY;

        String fareId = route + ":" + firstStop + "-" + lastStop;

        for (FareRuleSet ruleSet : fareRules) {
            if (ruleSet.getFareAttribute().getId().getId().equals(fareId)) {
                cost = ruleSet.getFareAttribute().getPrice();
                break;
            }
        }

        if (cost == Float.POSITIVE_INFINITY) {
            LOG.warn("Can't find price for " + firstStop + " to " + lastStop + " operated on " + route);

            return cost;
        }

        if (entranceFee) cost += 90f; /* TODO: Configurable? */

        return cost;
    }

    @Override
    public Fare getCost(Path<TripSchedule> path, TransitLayer transitLayer) {
        Currency euros = Currency.getInstance("EUR");
        // Use the usual process from the default fare service, but force the currency to Euros.
        // The default process assumes there is only one currency per set of fare rules and looks at any old rule to
        // guess what the currency is. This doesn't work on the Dutch data which has distances mixed in with Euros to
        // account for distance-derived fares.
        Fare fare = super.getCost(path, transitLayer);
        if (fare != null) {
            for (Money money : fare.fare.values()) {
                money.setCurrency(euros);
            }
        }
        return fare;
    }

    @Override
    protected float getLowestCost(FareType fareType, List<Ride> rides, Collection<FareRuleSet> fareRules) {

        float cost = 0f;

	    int units = 0;
        int prevSumUnits = 0;

        boolean mustHaveCheckedOut = false;
        String startTariefEenheden = null;
        String endTariefEenheden = null;
        FeedScopedId lastAgencyId = null;
        String lastFareZone = null;

        long alightedEasyTrip = 0;
        long alightedTariefEenheden = 0;

        for (Ride ride : rides) {
            LOG.trace(String.format("%s %s %s %s %s %s", ride.startZone, ride.endZone, ride.firstStop, ride.lastStop, ride.route, ride.agency));

            if (ride.agency.getFeedId().equals("IFF")) {
                LOG.trace("1. Trains");
		        /* In Reizen op Saldo we will try to fares as long as possible. */

                /* If our previous agency isn't this agency, then we must have checked out */
                mustHaveCheckedOut |= !ride.agency.equals(lastAgencyId);

                /* When a user has checked out, we first calculate the units made until then. */
                if (mustHaveCheckedOut && lastAgencyId != null) {
                    LOG.trace("2. Must have checked out from a station");
                    UnitsFareZone unitsFareZone = getUnitsByZones(lastAgencyId, startTariefEenheden, endTariefEenheden, fareRules);
                    if (unitsFareZone == null) return Float.POSITIVE_INFINITY;
                    lastFareZone = unitsFareZone.fareZone;
                    units += unitsFareZone.units;
                    startTariefEenheden = ride.startZone;
                    mustHaveCheckedOut = false;
                }

        		/* The entrance Fee applies if the transfer time ends before the new trip starts. */
                if ((alightedTariefEenheden + TRANSFER_DURATION) < ride.startTime) {
                    LOG.trace("3. Exceeded Transfer Time");
                    cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);
                    if (cost == Float.POSITIVE_INFINITY) return cost;

                    startTariefEenheden = ride.startZone;
                    units = 0;
                    prevSumUnits = 0;
                    mustHaveCheckedOut = false;

                } else if (!ride.agency.equals(lastAgencyId)) {
                    LOG.trace("4. Swiched Rail Agency");

                    cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);
                    if (cost == Float.POSITIVE_INFINITY) return cost;

                    prevSumUnits += units;
                    units = 0;
                    startTariefEenheden = ride.startZone;
                }

                alightedTariefEenheden = ride.endTime;
                endTariefEenheden = ride.endZone;
                lastAgencyId = ride.agency;

            } else {
                LOG.trace("5. Easy Trip");

                /* We are now on Easy Trip, so we must have checked-out from Reizen op Saldo, if we were on it */
                mustHaveCheckedOut = (startTariefEenheden != null);

                /* The entranceFee applies if the transfer time ends before the new trip starts. */
                boolean entranceFee = ((alightedEasyTrip + TRANSFER_DURATION) < ride.startTime);

                /* EasyTrip will always calculate its price per leg */
                cost += getEasyTripFareByLineFromTo(ride.route.getId(), ride.startZone, ride.endZone, entranceFee, fareRules);
                if (cost == Float.POSITIVE_INFINITY) return cost;

                alightedEasyTrip = ride.endTime;
            }
        }

        LOG.trace("6. Final");
        if (lastAgencyId != null) {
            UnitsFareZone unitsFareZone = getUnitsByZones(lastAgencyId, startTariefEenheden, endTariefEenheden, fareRules);
            if (unitsFareZone == null) return Float.POSITIVE_INFINITY;

            lastFareZone = unitsFareZone.fareZone;
            units += unitsFareZone.units;
            cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);
        }

        if (cost == Float.POSITIVE_INFINITY) return cost;

        return cost / 100f;
    }
}
