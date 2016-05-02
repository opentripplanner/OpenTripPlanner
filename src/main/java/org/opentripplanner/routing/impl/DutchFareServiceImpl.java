/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.xml.xsi.XSISimpleTypes;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Fare.FareType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DutchFareServiceImpl extends DefaultFareServiceImpl {

    public DutchFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
        addFareRules(FareType.regular, regularFareRules);
    }

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(DutchFareServiceImpl.class);

    public static final int TRANSFER_DURATION = 60 * 35; /* tranfers within 35 min won't require a new base fare */

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

    private UnitsFareZone getUnitsByZones(String startZone, String endZone, Collection<FareRuleSet> fareRules) {
        P2<String> od = new P2<String>(startZone, endZone);

        LOG.warn("Search " + startZone + " and " + endZone);

        for (FareRuleSet ruleSet : fareRules) {
            /* TODO: agency selectie moet hier ook nog, dordrecht geldermalsen */
            if (ruleSet.getOriginDestinations().contains(od)) {
                String fareId = ruleSet.getFareAttribute().getId().getId();
                String[] parts = fareId.split(":");
                String fareZone = parts[0] + ':' + parts[1];

                LOG.warn("Between " + startZone + " and " + endZone + ": " + (int) ruleSet.getFareAttribute().getPrice() + " (" + fareZone + ")");
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

        float cost = 0f;

        String fareId = fareZone + ":" + units;
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
                    break;
                }
            }

            LOG.warn("Can't find price for " + fareZone + " with " + prevSumUnits + " units");
        }

        return cost;
    }

    private float getEasyTripFareByLineFromTo(AgencyAndId route, String firstStop, String lastStop,
                                              boolean entranceFee, Collection<FareRuleSet> fareRules) {

        float cost = 0f;

        for (FareRuleSet ruleSet : fareRules) {
            Set<String> agencies = new HashSet<>();
            agencies.add(route.getAgencyId());

            Set<AgencyAndId> routes = new HashSet<>();
            routes.add(route);

            if (ruleSet.matches(agencies, firstStop, lastStop, new HashSet<String>(), routes)) {
                cost = ruleSet.getFareAttribute().getPrice();
                break;
            }
        }

        if (entranceFee) cost += 0.89f; /* TODO: Configurable? */

        return cost;
    }

    @Override
    protected float getLowestCost(FareType fareType, List<Ride> rides,
                                  Collection<FareRuleSet> fareRules) {

        float cost = 0f;

	    int units = 0;
        int prevSumUnits = 0;

        boolean mustHaveCheckedOut = false;
        String startTariefEenheden = null;
        String endTariefEenheden = null;
        String lastAgencyId = null;
        String lastFareZone = null;

        long alightedEasyTrip = 0;
        long alightedTariefEenheden = 0;

        for (Ride ride : rides) {
            LOG.warn(String.format("%s %s %s %s %s %s", ride.startZone, ride.endZone, ride.firstStop, ride.lastStop, ride.route, ride.agency));

            if (ride.agency.startsWith("IFF:")) {
                LOG.warn("1. NS");
		        /* In Reizen op Saldo we will try to fares as long as possible. */

                /* If our previous agency isn't this agency, then we must have checked out */
                mustHaveCheckedOut |= !ride.agency.equals(lastAgencyId);

                /* When a user has checked out, we first calculate the units made until then. */
                if (mustHaveCheckedOut && lastAgencyId != null) {
                    LOG.warn("2. Must Have Checked Out");
                    UnitsFareZone unitsFareZone = getUnitsByZones(startTariefEenheden, endTariefEenheden, fareRules);
                    if (unitsFareZone == null) return Float.POSITIVE_INFINITY;
                    lastFareZone = unitsFareZone.fareZone;
                    units += unitsFareZone.units;
                    startTariefEenheden = ride.startZone;
                    mustHaveCheckedOut = false;
                }

        		/* The entrance Fee applies if the transfer time ends before the new trip starts. */
                if ((alightedTariefEenheden + TRANSFER_DURATION) < ride.startTime) {
                    LOG.warn("3. Exceeded Transfer Time");
                    cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);
                    startTariefEenheden = ride.startZone;
                    units = 0;
                    prevSumUnits = 0;
                    mustHaveCheckedOut = false;

                } else if (!ride.agency.equals(lastAgencyId)) {
                    LOG.warn("4. Swiched Rail Agency");

                    cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);
                    units = 0;
                    startTariefEenheden = ride.startZone;
                }

                alightedTariefEenheden = ride.endTime;
                endTariefEenheden = ride.endZone;
                lastAgencyId = ride.agency;

            } else {
                LOG.warn("5. Easy Trip");

                /* We are now on Easy Trip, so we must have checked-out from Reizen op Saldo, if we were on it */
                mustHaveCheckedOut = (startTariefEenheden != null);

                /* The entranceFee applies if the transfer time ends before the new trip starts. */
                boolean entranceFee = ((alightedEasyTrip + TRANSFER_DURATION) < ride.startTime);

                /* EasyTrip will always calculate its price per leg */
                cost += getEasyTripFareByLineFromTo(ride.route, ride.firstStop.toString(), ride.lastStop.toString(), entranceFee, fareRules);

                alightedEasyTrip = ride.endTime;
            }
        }

        LOG.warn("6. Final");
        UnitsFareZone unitsFareZone = getUnitsByZones(startTariefEenheden, endTariefEenheden, fareRules);
        if (unitsFareZone == null) return Float.POSITIVE_INFINITY;

        lastFareZone = unitsFareZone.fareZone;
        units += unitsFareZone.units;
        cost += getCostByUnits(lastFareZone, units, prevSumUnits, fareRules);

        return cost / 100f;
    }
}
