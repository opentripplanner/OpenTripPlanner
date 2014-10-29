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

public class Stop extends Entity {

    public String stop_id;
    public String stop_code;
    public String stop_name;
    public String stop_desc;
    public double stop_lat;
    public double stop_lon;
    public String zone_id;
    public String stop_url;
    public int    location_type;
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
            s.stop_id   = getStringField("stop_id");
            s.stop_code = getStringField("stop_code");
            s.stop_name = getStringField("stop_name");
            s.stop_desc = getStringField("stop_desc");
            s.stop_lat  = getDoubleField("stop_lat");
            s.stop_lon  = getDoubleField("stop_lon");
            s.zone_id   = getStringField("zone_id");
            s.stop_url  = getStringField("stop_url");
            s.location_type  = getIntField("location_type");
            s.parent_station = getStringField("parent_station");
            s.stop_timezone  = getStringField("stop_timezone");
            s.wheelchair_boarding = getStringField("wheelchair_boarding");
            return s;
        }

    }

}
