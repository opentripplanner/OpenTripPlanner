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

import org.opentripplanner.model.Trip;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class TripMapper {

    private final RouteMapper routeMapper;

    private Map<org.onebusaway.gtfs.model.Trip, Trip> mappedTrips = new HashMap<>();

    TripMapper(RouteMapper routeMapper) {
        this.routeMapper = routeMapper;
    }

    Collection<Trip> map(Collection<org.onebusaway.gtfs.model.Trip> trips) {
        return MapUtils.mapToList(trips, this::map);
    }

    Trip map(org.onebusaway.gtfs.model.Trip orginal) {
        return orginal == null ? null : mappedTrips.computeIfAbsent(orginal, this::doMap);
    }

    private Trip doMap(org.onebusaway.gtfs.model.Trip rhs) {
        Trip lhs = new Trip();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setRoute(routeMapper.map(rhs.getRoute()));
        lhs.setServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
        lhs.setTripShortName(rhs.getTripShortName());
        lhs.setTripHeadsign(rhs.getTripHeadsign());
        lhs.setRouteShortName(rhs.getRouteShortName());
        lhs.setDirectionId(rhs.getDirectionId());
        lhs.setBlockId(rhs.getBlockId());
        lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
        lhs.setWheelchairAccessible(rhs.getWheelchairAccessible());
        lhs.setTripBikesAllowed(rhs.getTripBikesAllowed());
        lhs.setBikesAllowed(rhs.getBikesAllowed());
        lhs.setFareId(rhs.getFareId());

        return lhs;
    }

}
