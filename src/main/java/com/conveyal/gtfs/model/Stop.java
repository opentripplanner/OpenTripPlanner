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

package com.conveyal.gtfs.model;

import com.conveyal.gtfs.GTFSFeed;

import java.io.IOException;
import java.net.URL;

public class Stop extends Entity {

    public String stop_id;
    public String stop_code;
    public String stop_name;
    public String stop_desc;
    public double stop_lat;
    public double stop_lon;
    public String zone_id;
    public URL    stop_url;
    public int    location_type;
    public String parent_station;
    public String stop_timezone;
    public String wheelchair_boarding;

    public static class Loader extends Entity.Loader<Stop> {

        public Loader(GTFSFeed feed) {
            super(feed, "stops");
        }

        @Override
        public void loadOneRow() throws IOException {
            Stop s = new Stop();
            s.stop_id   = getStringField("stop_id", true);
            s.stop_code = getStringField("stop_code", false);
            s.stop_name = getStringField("stop_name", true);
            s.stop_desc = getStringField("stop_desc", false);
            s.stop_lat  = getDoubleField("stop_lat", true, -90D, 90D);
            s.stop_lon  = getDoubleField("stop_lon", true, -180D, 180D);
            s.zone_id   = getStringField("zone_id", false);
            s.stop_url  = getUrlField("stop_url", false);
            s.location_type  = getIntField("location_type", false, 0, 1);
            s.parent_station = getStringField("parent_station", false);
            s.stop_timezone  = getStringField("stop_timezone", false);
            s.wheelchair_boarding = getStringField("wheelchair_boarding", false);
            s.feed = feed;
            /* TODO check ref integrity later, this table self-references via parent_station */

            feed.stops.put(s.stop_id, s);
        }

    }

}
