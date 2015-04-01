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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeattleFareServiceImpl extends DefaultFareServiceImpl {
    private static final long serialVersionUID = 1L;

    // Agency IDs defined in King Metro Transit GTFS
    public static final String KCM_EOS_AGENCY_ID = "EOS";

    public static final String KCM_KCM_AGENCY_ID = "KCM";

    public static final String KCM_ST_AGENCY_ID = "ST";

    public static final String KCM_KMD_AGENCY_ID = "KMD";

    // Agency IDs defined in Pierce Transit GTFS
    public static final String PT_PT_AGENCY_ID = "3";

    public static final String PT_ST_AGENCY_ID = "40";

    // Agency IDs defined in Sound Transit GTFS
    public static final String ST_ST_AGENCY_ID = "SoundTransit";

    // Agency IDs defined in Community Transit GTFS
    public static final String CT_CT_AGENCY_ID = "29";

    public static final int TRANSFER_DURATION_SEC = 7200;

    // Fallback in case no rules apply for an agency
    private Map<T2<FareType, String>, Float> defaultFares = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceImpl.class);

    public SeattleFareServiceImpl(Collection<FareRuleSet> regularFareRules,
            Collection<FareRuleSet> youthFareRules, Collection<FareRuleSet> seniorFareRules) {
        super();
        addFareRules(FareType.regular, regularFareRules);
        addFareRules(FareType.youth, youthFareRules);
        addFareRules(FareType.senior, seniorFareRules);
    }

    public void addDefaultFare(FareType fareType, String agencyId, float cost) {
        defaultFares.put(new T2<FareType, String>(fareType, agencyId), cost);
    }

    @Override
    protected float getLowestCost(FareType fareType, List<Ride> rides,
            Collection<FareRuleSet> fareRules) {

        // Split rides per agency
        List<List<Ride>> ridesPerAgency = new ArrayList<List<Ride>>();
        String lastAgency = null;
        List<Ride> currentRides = null;
        for (Ride ride : rides) {
            if (ride.agency != lastAgency) {
                currentRides = new ArrayList<Ride>();
                ridesPerAgency.add(currentRides);
                lastAgency = ride.agency;
            }
            currentRides.add(ride);
        }

        LOG.debug("=== Rides for fare class {} ===", fareType);
        for (List<Ride> ridesForAgency : ridesPerAgency) {
            LOG.debug("Ride for agency {} : {}", ridesForAgency.get(0).agency,
                    Arrays.toString(ridesForAgency.toArray()));
        }

        float currentCost = 0f;
        float totalCost = 0f;
        long lastStartSec = 0L;
        for (List<Ride> ridesForAgency : ridesPerAgency) {

            String agencyId = ridesForAgency.get(0).agency;
            long startSec = ridesForAgency.get(0).startTime; // seconds
            float costForAgency = super.getLowestCost(fareType, ridesForAgency, fareRules);

            if (costForAgency == Float.POSITIVE_INFINITY) {
                Float def = defaultFares.get(new T2<FareType, String>(fareType, agencyId));
                if (def == null) {
                    LOG.error("No fares and no fallback for class {}, agency {}, rides {}",
                            fareType, agencyId, ridesForAgency);
                    return Float.POSITIVE_INFINITY;
                }
                costForAgency = def;
            }
            LOG.debug("Agency {} cost is {}", agencyId, costForAgency);

            // Check for transfer
            if (startSec < lastStartSec + TRANSFER_DURATION_SEC) {
                // Transfer OK
                if (costForAgency > currentCost) {
                    // Add top-up
                    float deltaCost = costForAgency - currentCost;
                    totalCost += deltaCost;
                    // Record max ticket price for current transfer
                    currentCost = costForAgency;
                    LOG.debug("Transfer, additional cost is {}, total is {}", deltaCost, totalCost);
                } else {
                    LOG.debug("New ticket cost lower than current {}", currentCost);
                }
                // TODO Record discount
            } else {
                // New one needed
                currentCost = costForAgency;
                totalCost += costForAgency;
                LOG.debug("New ticket, cost is {}, total is {}", costForAgency, totalCost);
                lastStartSec = startSec;
            }
        }

        return totalCost;
    }
}
