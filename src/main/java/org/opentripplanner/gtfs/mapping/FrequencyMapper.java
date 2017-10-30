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

import org.opentripplanner.model.Frequency;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class FrequencyMapper {
    private final TripMapper tripMapper;

    private Map<org.onebusaway.gtfs.model.Frequency, Frequency> mappedFrequencys = new HashMap<>();

    FrequencyMapper(TripMapper tripMapper) {
        this.tripMapper = tripMapper;
    }

    Collection<Frequency> map(Collection<org.onebusaway.gtfs.model.Frequency> allFrequencys) {
        return MapUtils.mapToList(allFrequencys, this::map);
    }

    Frequency map(org.onebusaway.gtfs.model.Frequency orginal) {
        return orginal == null ? null : mappedFrequencys.computeIfAbsent(orginal, this::doMap);
    }

    private Frequency doMap(org.onebusaway.gtfs.model.Frequency rhs) {
        Frequency lhs = new Frequency();

        lhs.setId(rhs.getId());
        lhs.setTrip(tripMapper.map(rhs.getTrip()));
        lhs.setStartTime(rhs.getStartTime());
        lhs.setEndTime(rhs.getEndTime());
        lhs.setHeadwaySecs(rhs.getHeadwaySecs());
        lhs.setExactTimes(rhs.getExactTimes());
        lhs.setLabelOnly(rhs.getLabelOnly());

        return lhs;
    }
}
