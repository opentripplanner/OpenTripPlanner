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

package org.opentripplanner.api.adapters;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<MapType, Map<String, String>> {

    @Override
    public MapType marshal(Map<String, String> v) throws Exception {
        MapType MapType = new MapType();
        List<Entry> aList = MapType.getEntry();
        for (Map.Entry<String, String> e : v.entrySet()) {
            aList.add(new Entry(e.getKey(), e.getValue()));
        }
        return MapType;
    }

    @Override
    public Map<String, String> unmarshal(MapType v) throws Exception {
        Map<String, String> map = new TreeMap<String, String>();
        for (Entry e : v.getEntry()) {
            map.put(e.key, e.value);
        }
        return map;
    }
}