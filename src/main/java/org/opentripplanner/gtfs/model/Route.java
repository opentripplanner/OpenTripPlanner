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

import com.csvreader.CsvReader;

import java.io.IOException;
import java.util.Map;

public class Route extends Entity { // implements Entity.Factory<Route>

    public static final int BUS = 0;

    public String route_id;
    public String agency_id;
    public String route_short_name;
    public String route_long_name;
    public String route_desc;
    public int    route_type;
    public String route_url;
    public String route_color;
    public String route_text_color;

    @Override
    public String getKey() {
        return route_id;
    }

    public static class Factory extends Entity.Factory<Route> {

        public Factory() {
            tableName = "routes";
            requiredColumns = new String[] {"route_id", "agency_id"};
        }

        @Override
        public Route fromCsv() throws IOException {
            Route r = new Route();
            r.route_id         = getStrField("route_id");
            r.agency_id        = getStrField("agency_id");
            r.route_short_name = getStrField("route_short_name");
            r.route_long_name  = getStrField("route_long_name");
            r.route_desc       = getStrField("route_desc");
            r.route_type       = getIntField("route_type");
            r.route_url        = getStrField("route_url");
            r.route_color      = getStrField("route_color");
            r.route_text_color = getStrField("route_text_color");
            r.feedId = "FEED";
            return r;
        }

    }

}
