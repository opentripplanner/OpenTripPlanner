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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

    private Map<AgencyAndId, FareRuleSet> youthFareRules = new HashMap<AgencyAndId, FareRuleSet>();

    private Map<AgencyAndId, FareRuleSet> seniorFareRules = new HashMap<AgencyAndId, FareRuleSet>();

    @Override
    public FareService makeFareService() {

        /**
         * Various GTFS fares are missing default fares. We add them there, as standard FareRuleSet.
         * Updated to match March 1, 2015 fare changes from KCM and
         */

        // KCM - King County Metro
        // metro.kingcounty.gov/fares/
        // Data within King Metro GTFS
        addMissingFare(youthFareRules, 1.50f, SeattleFareServiceImpl.KCM_KCM_AGENCY_ID);
        addMissingFare(seniorFareRules, 1.00f, SeattleFareServiceImpl.KCM_KCM_AGENCY_ID);

        // EOS - Seattle Street Car
        // http://www.seattlestreetcar.org/ride_fares.htm
        // Data within King Metro GTFS
        addMissingFare(regularFareRules, 2.25f, SeattleFareServiceImpl.KCM_EOS_AGENCY_ID);
        addMissingFare(youthFareRules, 1.50f, SeattleFareServiceImpl.KCM_EOS_AGENCY_ID);
        addMissingFare(seniorFareRules, 1.00f, SeattleFareServiceImpl.KCM_EOS_AGENCY_ID);

        // Sound Transit Express Bus
        // http://www.soundtransit.org/Fares-and-Passes/ST-Express-bus-fares
        // Data within King Metro GTFS
        addMissingFare(youthFareRules, 1.25f, SeattleFareServiceImpl.KCM_ST_AGENCY_ID);
        addMissingFare(seniorFareRules, 0.75f, SeattleFareServiceImpl.KCM_ST_AGENCY_ID);
        // Data within Pierce Transit GTFS
        // TODO Some lines crosses zone, fare should be higher in this case
        // but we do not have zone info for each stops
        addMissingFare(regularFareRules, 2.50f, SeattleFareServiceImpl.PT_ST_AGENCY_ID);
        addMissingFare(youthFareRules, 1.25f, SeattleFareServiceImpl.PT_ST_AGENCY_ID);
        addMissingFare(seniorFareRules, 0.75f, SeattleFareServiceImpl.PT_ST_AGENCY_ID);
        // Data within Sound Transit GTFS
        // TLINK only
        addMissingFare(youthFareRules, 1.25f, SeattleFareServiceImpl.PT_ST_AGENCY_ID, "TLZ", "TLZ");
        addMissingFare(seniorFareRules, 0.75f, SeattleFareServiceImpl.PT_ST_AGENCY_ID, "TLZ", "TLZ");

        // Pierce Transit
        // http://www.piercetransit.org/fares/
        // Data within Pierce Transit GTFS
        addMissingFare(regularFareRules, 2.00f, SeattleFareServiceImpl.PT_PT_AGENCY_ID);
        addMissingFare(youthFareRules, 0.75f, SeattleFareServiceImpl.PT_PT_AGENCY_ID);
        addMissingFare(seniorFareRules, 0.75f, SeattleFareServiceImpl.PT_PT_AGENCY_ID);

        // Community Transit
        // http://www.communitytransit.org/reducedfare/
        // Data within Community Transit GTFS
        // TODO Higher fare for buses Seattle area depending on zone.
        // But zone are not defined in the GTFS data
        addMissingFare(regularFareRules, 2.00f, SeattleFareServiceImpl.CT_CT_AGENCY_ID);
        addMissingFare(youthFareRules, 1.50f, SeattleFareServiceImpl.CT_CT_AGENCY_ID);
        addMissingFare(seniorFareRules, 1.00f, SeattleFareServiceImpl.CT_CT_AGENCY_ID);

        // Sound Transit Bus Express, duplicate fares 30 and 36 with their rules
        AgencyAndId fare30 = new AgencyAndId(SeattleFareServiceImpl.ST_ST_AGENCY_ID, "30");
        AgencyAndId fare36 = new AgencyAndId(SeattleFareServiceImpl.ST_ST_AGENCY_ID, "36");
        for (FareRuleSet fareRule : regularFareRules.values()) {
            if (fareRule.getFareAttribute().getId().equals(fare30)) {
                duplicateFareRuleSet(youthFareRules, fareRule, 1.25f);
                duplicateFareRuleSet(seniorFareRules, fareRule, 0.75f);
            } else if (fareRule.getFareAttribute().getId().equals(fare36)) {
                duplicateFareRuleSet(youthFareRules, fareRule, 2.50f);
                duplicateFareRuleSet(seniorFareRules, fareRule, 1.50f);
            }
        }

        SeattleFareServiceImpl fareService = new SeattleFareServiceImpl(regularFareRules.values(),
                youthFareRules.values(), seniorFareRules.values());

        // Add fallbacks. Some rules are missing, for example from zone 1 to 21: no rules apply.
        // Please note that those fares only apply if no other rule apply.
        // We pick one zone only
        fareService.addDefaultFare(FareType.regular, SeattleFareServiceImpl.KCM_KCM_AGENCY_ID,
                2.50f);

        return fareService;
    }

    private static int internalFareId = 0;

    private void duplicateFareRuleSet(Map<AgencyAndId, FareRuleSet> fareRules,
            FareRuleSet fareRule, float price) {
        FareAttribute fare = createInternalFareAttribute(price);
        FareRuleSet newFareRule = new FareRuleSet(fare);
        for (P2<String> originDestZone : fareRule.getOriginDestinations()) {
            newFareRule.addOriginDestination(originDestZone.first, originDestZone.second);
        }
        fareRules.put(fare.getId(), newFareRule);
    }

    private void addMissingFare(Map<AgencyAndId, FareRuleSet> fareRules, float price,
            String agencyId) {
        addMissingFare(fareRules, price, agencyId, null, null);
    }

    private void addMissingFare(Map<AgencyAndId, FareRuleSet> fareRules, float price,
            String agencyId, String originZone, String destZone) {
        FareAttribute mFare = createInternalFareAttribute(price);
        FareRuleSet mFareRules = new FareRuleSet(mFare);
        if (originZone != null && destZone != null)
            mFareRules.addOriginDestination(originZone, destZone);
        mFareRules.setAgency(agencyId);
        fareRules.put(mFare.getId(), mFareRules);
    }

    private FareAttribute createInternalFareAttribute(float price) {
        FareAttribute fare = new FareAttribute();
        fare.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
        fare.setCurrencyType("USD");
        fare.setPrice(price);
        fare.setId(new AgencyAndId(SeattleFareServiceImpl.KCM_EOS_AGENCY_ID, "internal_"
                + internalFareId));
        internalFareId++;
        return fare;
    }

    @Override
    public void processGtfs(GtfsRelationalDao dao) {

        /*
         * Sort all fares based on their agency. TODO With the new GTFS library, this code may be
         * removed. We should simply read fare attribute "agency" field (extention).
         */
        Map<String, Set<FareAttribute>> fareAttributesPerAgency = new HashMap<String, Set<FareAttribute>>();
        Map<String, Set<FareRule>> fareRulesPerAgency = new HashMap<String, Set<FareRule>>();
        for (FareAttribute fareAttribute : dao.getAllFareAttributes()) {
            String fareAgencyId;
            String mainAgencyId = fareAttribute.getId().getAgencyId();
            if (SeattleFareServiceImpl.KCM_EOS_AGENCY_ID.equals(mainAgencyId)) {
                // Split fare according to agency
                int id = Integer.parseInt(fareAttribute.getId().getId());
                if (id < 10) {
                    fareAgencyId = SeattleFareServiceImpl.KCM_KCM_AGENCY_ID;
                    fareAttribute.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
                } else {
                    fareAgencyId = SeattleFareServiceImpl.KCM_ST_AGENCY_ID;
                    // TODO Check this for ST
                    fareAttribute.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
                }
                fareAgencyId = SeattleFareServiceImpl.KCM_KCM_AGENCY_ID;
            } else {
                fareAgencyId = mainAgencyId;
            }
            Set<FareAttribute> fareAttributes = fareAttributesPerAgency.get(fareAgencyId);
            if (fareAttributes == null) {
                fareAttributes = new HashSet<>();
                fareAttributesPerAgency.put(fareAgencyId, fareAttributes);
            }
            fareAttributes.add(fareAttribute);
            for (FareRule fareRule : dao.getFareRulesForFareAttribute(fareAttribute)) {
                Set<FareRule> fareRules = fareRulesPerAgency.get(fareAgencyId);
                if (fareRules == null) {
                    fareRules = new HashSet<>();
                    fareRulesPerAgency.put(fareAgencyId, fareRules);
                }
                fareRules.add(fareRule);
            }
        }

        for (Map.Entry<String, Set<FareAttribute>> kv : fareAttributesPerAgency.entrySet()) {
            super.fillFareRules(kv.getKey(), kv.getValue(), fareRulesPerAgency.get(kv.getKey()),
                    regularFareRules);
        }
    }

    @Override
    public void configure(JsonNode config) {
        // No config for the moment
    }
}
