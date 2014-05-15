/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.onebusaway.gtfs.model.Stop;

public class StopAdapter extends XmlAdapter<StopType, Stop> {

    @Override
    public Stop unmarshal(StopType arg) throws Exception {
        if (arg == null) {
            return null;
        }
        Stop a = new Stop();
        a.setId(arg.id);
        a.setName(arg.stopName);
        a.setCode(arg.stopCode);
        a.setDesc(arg.stopDesc);
        a.setLat(arg.stopLat);
        a.setLon(arg.stopLon);
        a.setZoneId(arg.zoneId);
        a.setUrl(arg.stopUrl);
        a.setLocationType(arg.locationType);
        a.setParentStation(arg.parentStation);
        a.setWheelchairBoarding(arg.wheelchairBoarding);
        a.setDirection(arg.direction);
        return new Stop(a);
    }

    @Override
    public StopType marshal(Stop arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new StopType(arg);
    }

}
