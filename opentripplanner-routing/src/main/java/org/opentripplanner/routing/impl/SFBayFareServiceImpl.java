package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFBayFareServiceImpl extends DefaultFareServiceImpl {

    public SFBayFareServiceImpl(
            HashMap<AgencyAndId, FareRuleSet>   fareRules,
            HashMap<AgencyAndId, FareAttribute> fareAttributes) {
        super(fareRules, fareAttributes);
    }

    private static final long serialVersionUID = 20120229L;
    private static final Logger LOG = LoggerFactory.getLogger(SFBayFareServiceImpl.class);

    public static final int SFMTA_TRANSFER_DURATION = 60 * 90;
    public static final int BART_TRANSFER_DURATION =  60 * 60;
    public static final float SFMTA_BASE_FARE = 2.00f;
    public static final float CABLE_CAR_FARE = 5.00f;
    public static final float AIRBART_FARE = 3.00f;
    public static final float SFMTA_BART_TRANSFER_FARE = 1.75f;
    public static final Set<String> SFMTA_BART_TRANSFER_STOPS = new HashSet<String>(Arrays.asList(
            "EMBR", "MONT", "POWL", "CIVC", "16TH", "24TH", "GLEN", "BALB", "DALY"));
    public static final String SFMTA_BART_FREE_TRANSFER_STOP = "DALY";
    
    @Override
    public float getLowestCost(List<Ride> rides) {
        List<Ride> bartBlock = null;
        Long sfmtaTransferIssued = null;
        Long alightedBart = null;
        String alightedBartStop = null;
        float cost = 0f;
        String agencyId = null;
        for (Ride ride : rides) {
            agencyId = ride.route.getAgencyId();
            if (agencyId.equals("BART")) {
                if (bartBlock == null) {
                    bartBlock = new ArrayList<Ride>();
                }
                bartBlock.add(ride);
                alightedBart = ride.endTime;
                alightedBartStop = ride.lastStop.getId().getId();
            } else { // non-BART agency
                if (bartBlock != null) {
                    // finalize outstanding bart block, if any
                    cost += calculateCost(bartBlock);
                    bartBlock = null;
                }
                if (agencyId.equals("SFMTA")) {
                    if (ride.classifier == TraverseMode.CABLE_CAR) {
                        // no transfers issued or accepted
                        cost += CABLE_CAR_FARE;
                    } else if (sfmtaTransferIssued == null || 
                        sfmtaTransferIssued + SFMTA_TRANSFER_DURATION < ride.endTime) {
                        sfmtaTransferIssued = ride.startTime;
                        if (alightedBart != null &&
                            alightedBart + BART_TRANSFER_DURATION > ride.startTime &&
                            SFMTA_BART_TRANSFER_STOPS.contains(alightedBartStop)) {
                            // discount for BART to Muni transfer
                            if (alightedBartStop.equals(SFMTA_BART_FREE_TRANSFER_STOP)) {
                                // no cost to ride Muni
                            } else {
                                cost += SFMTA_BART_TRANSFER_FARE;
                            }
                        } else { 
                            // no transfer, basic fare
                            cost += SFMTA_BASE_FARE; 
                        }
                    } else {
                        // SFMTA-SFMTA non-cable-car transfer within time limit, no cost
                    }
                } else if (agencyId.equals("AirBART")) {
                    cost += AIRBART_FARE;
                }
            }
        }
        if (bartBlock != null) {
            // finalize outstanding bart block, if any
            cost += calculateCost(bartBlock);
        }        
        return cost;
    }
    
}
