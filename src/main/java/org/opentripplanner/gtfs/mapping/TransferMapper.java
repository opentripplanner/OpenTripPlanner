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

import org.opentripplanner.model.Transfer;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class TransferMapper {
    private final RouteMapper routeMapper;

    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.Transfer, Transfer> mappedTransfers = new HashMap<>();

    public TransferMapper(RouteMapper routeMapper, StopMapper stopMapper, TripMapper tripMapper) {
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    Collection<Transfer> map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
        return MapUtils.mapToList(allTransfers, this::map);
    }

    Transfer map(org.onebusaway.gtfs.model.Transfer orginal) {
        return orginal == null ? null : mappedTransfers.computeIfAbsent(orginal, this::doMap);
    }

    private Transfer doMap(org.onebusaway.gtfs.model.Transfer rhs) {
        Transfer lhs = new Transfer();

        lhs.setId(rhs.getId());

        lhs.setId(rhs.getId());
        lhs.setFromStop(stopMapper.map(rhs.getFromStop()));
        lhs.setFromRoute(routeMapper.map(rhs.getFromRoute()));
        lhs.setFromTrip(tripMapper.map(rhs.getFromTrip()));
        lhs.setToStop(stopMapper.map(rhs.getToStop()));
        lhs.setToRoute(routeMapper.map(rhs.getToRoute()));
        lhs.setToTrip(tripMapper.map(rhs.getToTrip()));
        lhs.setTransferType(rhs.getTransferType());
        lhs.setMinTransferTime(rhs.getMinTransferTime());

        return lhs;
    }
}
