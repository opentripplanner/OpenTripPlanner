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
        // Data within King Metro GTFS
        // http://www.seattlestreetcar.org/slu.htm
        addMissingFare(fareRules, 2.50f, SeattleFareServiceImpl.EOS_AGENCY_ID);

        // Sound Transit Express Bus
        // Data within King Metro GTFS and Pierce Transit GTFS
        // www.soundtransit.org/Fares-and-Passes/ST-Express-bus-fares
        addMissingFare(fareRules, 2.50f, SeattleFareServiceImpl.ST_AGENCY_ID);

        // Pierce Transit
        // Data within Pierce Transit GTFS
        // http://www.piercetransit.org/fares/
        addMissingFare(fareRules, 2.00f, SeattleFareServiceImpl.PT_AGENCY_ID);

        // Community Transit
        // Data within Community Transit GTFS
        // http://www.communitytransit.org/reducedfare/
        // TODO Higher fare for buses Seattle area depending on zone.
        // But zone are not defined in the GTFS data
        addMissingFare(fareRules, 2.00f, SeattleFareServiceImpl.COMMUNITY_AGENCY_ID);

        return new SeattleFareServiceImpl(fareRules);
    }

    private static int internalFareId = 0;

    private void addMissingFare(Map<AgencyAndId, FareRuleSet> fareRules, float price,
            String agencyId) {
        FareAttribute mFare = new FareAttribute();
        mFare.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
        mFare.setCurrencyType("USD");
        mFare.setPrice(price);
        mFare.setId(new AgencyAndId(SeattleFareServiceImpl.EOS_AGENCY_ID, "internal_"
                + internalFareId));
        internalFareId++;
        FareRuleSet mFareRules = new FareRuleSet(mFare);
        mFareRules.setAgency(agencyId);
        mFareRules.setHasRule(true);
        fareRules.put(mFare.getId(), mFareRules);
    }

    @Override
    public void setDao(GtfsRelationalDao dao) {

        String mainAgencyId = null;
        for (FareAttribute fareAttribute : dao.getAllFareAttributes()) {
            mainAgencyId = fareAttribute.getId().getAgencyId();
            break;
        }

        Set<FareAttribute> filteredFareAttributes = new HashSet<FareAttribute>();
        Set<FareRule> filteredFareRules = new HashSet<FareRule>();
        String fareAgencyId = null;
        if (SeattleFareServiceImpl.EOS_AGENCY_ID.equals(mainAgencyId)) {
            // For KCM, only read KCM fares
            // TODO Remove with new GTFS lib, read fareAttribute.agency_id
            for (FareAttribute fareAttribute : dao.getAllFareAttributes()) {
                int id = Integer.parseInt(fareAttribute.getId().getId());
                if (id < 10) {
                    fareAttribute.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
                    filteredFareAttributes.add(fareAttribute);
                } // ST fare, skipped
            }
            fareAgencyId = SeattleFareServiceImpl.KCM_AGENCY_ID;
        } else {
            filteredFareAttributes.addAll(dao.getAllFareAttributes());
            fareAgencyId = mainAgencyId;
        }

        // Only process rules from filtered fares
        for (FareRule fareRule : dao.getAllFareRules()) {
            if (filteredFareAttributes.contains(fareRule.getFare())) {
                filteredFareRules.add(fareRule);
            }
        }

        super.fillFareRules(fareAgencyId, filteredFareAttributes, filteredFareRules, fareRules);
    }
}
