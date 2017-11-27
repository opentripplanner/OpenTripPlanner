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

import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ShapePointMapper {
    private Map<org.onebusaway.gtfs.model.ShapePoint, ShapePoint> mappedShapePoints = new HashMap<>();

    Collection<ShapePoint> map(Collection<org.onebusaway.gtfs.model.ShapePoint> allShapePoints) {
        return MapUtils.mapToList(allShapePoints, this::map);
    }

    ShapePoint map(org.onebusaway.gtfs.model.ShapePoint orginal) {
        return orginal == null ? null : mappedShapePoints.computeIfAbsent(orginal, this::doMap);
    }

    private ShapePoint doMap(org.onebusaway.gtfs.model.ShapePoint rhs) {
        ShapePoint lhs = new ShapePoint();

        lhs.setShapeId(AgencyAndIdMapper.mapAgencyAndId(rhs.getShapeId()));
        lhs.setSequence(rhs.getSequence());
        lhs.setLat(rhs.getLat());
        lhs.setLon(rhs.getLon());
        lhs.setDistTraveled(rhs.getDistTraveled());

        // Skip mapping of proxy
        // private transient StopTimeProxy proxy;
        if (rhs.getProxy() != null) {
            throw new IllegalStateException("Did not expect proxy to be set! Data: " + rhs);
        }

        return lhs;
    }
}
