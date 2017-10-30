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

import org.opentripplanner.model.Agency;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class AgencyMapper {

    private final Map<org.onebusaway.gtfs.model.Agency, Agency> mappedAgencies = new HashMap<>();

    Collection<Agency> map(Collection<org.onebusaway.gtfs.model.Agency> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    Agency map(org.onebusaway.gtfs.model.Agency orginal) {
        return orginal == null ? null : mappedAgencies.computeIfAbsent(orginal, this::doMap);
    }

    private Agency doMap(org.onebusaway.gtfs.model.Agency rhs) {
        Agency lhs = new Agency();

        lhs.setId(rhs.getId());
        lhs.setName(rhs.getName());
        lhs.setUrl(rhs.getUrl());
        lhs.setTimezone(rhs.getTimezone());
        lhs.setLang(rhs.getLang());
        lhs.setPhone(rhs.getPhone());
        lhs.setFareUrl(rhs.getFareUrl());
        lhs.setBrandingUrl(rhs.getBrandingUrl());

        return lhs;
    }
}
