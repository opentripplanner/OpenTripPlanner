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

package org.opentripplanner.gtfs.model;

import java.io.IOException;
import java.util.Map;

public class Stop extends Entity {

    public String stop_id;
    public String stop_code;
    public String stop_name;
    public String stop_desc;
    public String stop_lat;
    public String stop_lon;
    public String zone_id;
    public String stop_url;
    public String location_type;
    public String parent_station;
    public String stop_timezone;
    public String wheelchair_boarding;

    @Override
    public String getKey() {
        return stop_id;
    }

    public static class Factory extends Entity.Factory<Stop> {

        public Factory() {
            tableName = "stops";
            requiredColumns = new String[] {"stop_id"};
        }

        @Override
        public Stop fromCsv() throws IOException {
            Stop s = new Stop();
            s.stop_id   = getStrField("stop_id");
            s.stop_code = getStrField("stop_code");
            s.stop_name = getStrField("stop_name");
            s.stop_desc = getStrField("stop_desc");
            s.stop_lat  = getStrField("stop_lat");
            s.stop_lon  = getStrField("stop_lon");
            s.zone_id   = getStrField("zone_id");
            s.stop_url  = getStrField("stop_url");
            s.location_type  = getStrField("location_type");
            s.parent_station = getStrField("parent_station");
            s.stop_timezone  = getStrField("stop_timezone");
            s.wheelchair_boarding = getStrField("wheelchair_boarding");
            return s;
        }

    }

}
