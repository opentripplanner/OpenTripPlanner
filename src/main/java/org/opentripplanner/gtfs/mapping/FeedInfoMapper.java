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

import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.ServiceDateMapper.mapServiceDate;

class FeedInfoMapper {
    private Map<org.onebusaway.gtfs.model.FeedInfo, FeedInfo> mappedFeedInfos = new HashMap<>();

    Collection<FeedInfo> map(Collection<org.onebusaway.gtfs.model.FeedInfo> feedInfos) {
        return feedInfos == null ? null : MapUtils.mapToList(feedInfos, this::map);
    }

    FeedInfo map(org.onebusaway.gtfs.model.FeedInfo orginal) {
        return orginal == null ? null : mappedFeedInfos.computeIfAbsent(orginal, this::doMap);
    }

    private FeedInfo doMap(org.onebusaway.gtfs.model.FeedInfo rhs) {
        FeedInfo lhs = new FeedInfo();

        lhs.setId(rhs.getId());
        lhs.setPublisherName(rhs.getPublisherName());
        lhs.setPublisherUrl(rhs.getPublisherUrl());
        lhs.setLang(rhs.getLang());
        lhs.setStartDate(mapServiceDate(rhs.getStartDate()));
        lhs.setEndDate(mapServiceDate(rhs.getEndDate()));
        lhs.setVersion(rhs.getVersion());

        return lhs;
    }
}
