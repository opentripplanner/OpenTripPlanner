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
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

    @Override
    public FareService makeFareService() {

        /**
         * Various GTFS fares are missing default fares. We add them there, as standard FareRuleSet.
         */

        // EOS - Seattle Street Car
        // http://www.seattlestreetcar.org/slu.htm
        // Data within King Metro GTFS
        addMissingFare(fareRules, 2.50f, SeattleFareServiceImpl.KCM_EOS_AGENCY_ID);

        // Sound Transit Express Bus
        // www.soundtransit.org/Fares-and-Passes/ST-Express-bus-fares
        // Data within King Metro GTFS
        addMissingFare(fareRules, 2.50f, SeattleFareServiceImpl.KCM_ST_AGENCY_ID);
        // Data within Pierce Transit GTFS
        // TODO Some lines crosses zone, fare should be higher in this case
        // but we do not have zone info for each stops
        addMissingFare(fareRules, 2.50f, SeattleFareServiceImpl.PT_ST_AGENCY_ID);

        // Pierce Transit
        // http://www.piercetransit.org/fares/
        // Data within Pierce Transit GTFS
        addMissingFare(fareRules, 2.00f, SeattleFareServiceImpl.PT_PT_AGENCY_ID);

        // Community Transit
        // http://www.communitytransit.org/reducedfare/
        // Data within Community Transit GTFS
        // TODO Higher fare for buses Seattle area depending on zone.
        // But zone are not defined in the GTFS data
        addMissingFare(fareRules, 2.00f, SeattleFareServiceImpl.CT_CT_AGENCY_ID);

        return new SeattleFareServiceImpl(fareRules);
    }

    private static int internalFareId = 0;

    private void addMissingFare(Map<AgencyAndId, FareRuleSet> fareRules, float price,
            String agencyId) {
        FareAttribute mFare = new FareAttribute();
        mFare.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
        mFare.setCurrencyType("USD");
        mFare.setPrice(price);
        mFare.setId(new AgencyAndId(SeattleFareServiceImpl.KCM_EOS_AGENCY_ID, "internal_"
                + internalFareId));
        internalFareId++;
        FareRuleSet mFareRules = new FareRuleSet(mFare);
        mFareRules.setAgency(agencyId);
        mFareRules.setHasRule(true);
        fareRules.put(mFare.getId(), mFareRules);
    }

    @Override
    public void setDao(GtfsRelationalDao dao) {
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
                    fareRules);
        }
    }
}
