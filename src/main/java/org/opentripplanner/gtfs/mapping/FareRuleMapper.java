/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FareRule;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class FareRuleMapper {

    private final RouteMapper routeMapper;

    private final FareAttributeMapper fareAttributeMapper;

    private Map<org.onebusaway.gtfs.model.FareRule, FareRule> mappedFareRules = new HashMap<>();

    FareRuleMapper(RouteMapper routeMapper, FareAttributeMapper fareAttributeMapper) {
        this.routeMapper = routeMapper;
        this.fareAttributeMapper = fareAttributeMapper;
    }

    Collection<FareRule> map(Collection<org.onebusaway.gtfs.model.FareRule> allFareRules) {
        return MapUtils.mapToList(allFareRules, this::map);
    }

    FareRule map(org.onebusaway.gtfs.model.FareRule orginal) {
        return orginal == null ? null : mappedFareRules.computeIfAbsent(orginal, this::doMap);
    }

    private FareRule doMap(org.onebusaway.gtfs.model.FareRule rhs) {
        FareRule lhs = new FareRule();

        lhs.setId(rhs.getId());
        lhs.setFare(fareAttributeMapper.map(rhs.getFare()));
        lhs.setRoute(routeMapper.map(rhs.getRoute()));
        lhs.setOriginId(rhs.getOriginId());
        lhs.setDestinationId(rhs.getDestinationId());
        lhs.setContainsId(rhs.getContainsId());

        return lhs;
    }
}
