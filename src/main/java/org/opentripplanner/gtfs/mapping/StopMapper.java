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

import org.opentripplanner.model.Stop;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

class StopMapper {
    private Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

    Collection<Stop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
        return MapUtils.mapToList(allStops, this::map);
    }

    Stop map(org.onebusaway.gtfs.model.Stop orginal) {
        return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
    }

    private Stop doMap(org.onebusaway.gtfs.model.Stop rhs) {
        Stop lhs = new Stop();

        lhs.setId(mapAgencyAndId(rhs.getId()));
        lhs.setName(rhs.getName());
        lhs.setLat(rhs.getLat());
        lhs.setLon(rhs.getLon());
        lhs.setCode(rhs.getCode());
        lhs.setDesc(rhs.getDesc());
        lhs.setZoneId(rhs.getZoneId());
        lhs.setUrl(rhs.getUrl());
        lhs.setLocationType(rhs.getLocationType());
        lhs.setParentStation(rhs.getParentStation());
        lhs.setWheelchairBoarding(rhs.getWheelchairBoarding());
        lhs.setDirection(rhs.getDirection());
        lhs.setTimezone(rhs.getTimezone());
        lhs.setVehicleType(rhs.getVehicleType());
        lhs.setPlatformCode(rhs.getPlatformCode());

        return lhs;
    }
}
