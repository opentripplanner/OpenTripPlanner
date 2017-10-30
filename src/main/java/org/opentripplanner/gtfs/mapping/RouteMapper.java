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

import org.opentripplanner.model.Route;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class RouteMapper {
    private final AgencyMapper agencyMapper;

    private final Map<org.onebusaway.gtfs.model.Route, Route> mappedRoutes = new HashMap<>();

    RouteMapper(AgencyMapper agencyMapper) {
        this.agencyMapper = agencyMapper;
    }

    Collection<Route> map(Collection<org.onebusaway.gtfs.model.Route> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    Route map(org.onebusaway.gtfs.model.Route orginal) {
        return orginal == null ? null : mappedRoutes.computeIfAbsent(orginal, this::doMap);
    }

    private Route doMap(org.onebusaway.gtfs.model.Route rhs) {
        Route lhs = new Route();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setAgency(agencyMapper.map(rhs.getAgency()));
        lhs.setShortName(rhs.getShortName());
        lhs.setLongName(rhs.getLongName());
        lhs.setType(rhs.getType());
        lhs.setDesc(rhs.getDesc());
        lhs.setUrl(rhs.getUrl());
        lhs.setColor(rhs.getColor());
        lhs.setTextColor(rhs.getTextColor());
        lhs.setRouteBikesAllowed(rhs.getRouteBikesAllowed());
        lhs.setBikesAllowed(rhs.getBikesAllowed());
        lhs.setSortOrder(rhs.getSortOrder());
        lhs.setBrandingUrl(rhs.getBrandingUrl());

        return lhs;
    }
}
