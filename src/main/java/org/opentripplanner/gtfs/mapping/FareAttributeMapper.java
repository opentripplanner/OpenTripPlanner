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

package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

class FareAttributeMapper {
    private Map<org.onebusaway.gtfs.model.FareAttribute, FareAttribute> mappedStops = new HashMap<>();

    Collection<FareAttribute> map(Collection<org.onebusaway.gtfs.model.FareAttribute> allStops) {
        return MapUtils.mapToList(allStops, this::map);
    }

    FareAttribute map(org.onebusaway.gtfs.model.FareAttribute orginal) {
        return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
    }

    private FareAttribute doMap(org.onebusaway.gtfs.model.FareAttribute rhs) {
        FareAttribute lhs = new FareAttribute();

        lhs.setId(mapAgencyAndId(rhs.getId()));
        lhs.setPrice(rhs.getPrice());
        lhs.setCurrencyType(rhs.getCurrencyType());
        lhs.setPaymentMethod(rhs.getPaymentMethod());
        lhs.setTransfers(rhs.getTransfers());
        lhs.setTransferDuration(rhs.getTransferDuration());
        lhs.setYouthPrice(rhs.getYouthPrice());
        lhs.setSeniorPrice(rhs.getSeniorPrice());
        lhs.setJourneyDuration(rhs.getJourneyDuration());

        return lhs;
    }
}
