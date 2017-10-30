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

import org.opentripplanner.model.Pathway;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class PathwayMapper {

    private final StopMapper stopMapper;

    private Map<org.onebusaway.gtfs.model.Pathway, Pathway> mappedPathways = new HashMap<>();

    PathwayMapper(StopMapper stopMapper) {
        this.stopMapper = stopMapper;
    }

    Collection<Pathway> map(Collection<org.onebusaway.gtfs.model.Pathway> allPathways) {
        return MapUtils.mapToList(allPathways, this::map);
    }

    Pathway map(org.onebusaway.gtfs.model.Pathway orginal) {
        return orginal == null ? null : mappedPathways.computeIfAbsent(orginal, this::doMap);
    }

    private Pathway doMap(org.onebusaway.gtfs.model.Pathway rhs) {
        Pathway lhs = new Pathway();

        lhs.setId(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
        lhs.setPathwayType(rhs.getPathwayType());
        lhs.setFromStop(stopMapper.map(rhs.getFromStop()));
        lhs.setToStop(stopMapper.map(rhs.getToStop()));
        lhs.setTraversalTime(rhs.getTraversalTime());
        lhs.setWheelchairTraversalTime(rhs.getWheelchairTraversalTime());

        return lhs;
    }
}
