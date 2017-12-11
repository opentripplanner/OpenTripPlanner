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

import com.google.common.collect.Iterables;

import java.util.List;

public class SeattleFareServiceImpl extends DefaultFareServiceImpl {
    private static final long serialVersionUID = 2L;

    private static final String KCM_FEED_ID = "1";
    private static final String KCM_AGENCY_ID = "1";

    @Override
    protected float addFares(List<Ride> ride0, List<Ride> ride1, float cost0, float cost1) {
        String feedId = ride0.get(0).firstStop.getId().getAgencyId();
        String agencyId = ride0.get(0).agency;
        if (KCM_FEED_ID.equals(feedId) && KCM_AGENCY_ID.equals(agencyId)) {
            for (Ride r : Iterables.concat(ride0, ride1)) {
                if (!isCorrectAgency(r, feedId, agencyId)) {
                    return cost0 + cost1;
                }
            }
            return Math.max(cost0, cost1);
        }
        return cost0 + cost1;
    }

    private static boolean isCorrectAgency(Ride r, String feedId, String agencyId) {
        String rideFeedId = r.firstStop.getId().getAgencyId();
        String rideAgencyId = r.agency;
        return feedId.equals(rideFeedId) && agencyId.equals(rideAgencyId);
    }

}
