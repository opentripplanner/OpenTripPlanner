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

public class Route extends Entity { // implements Entity.Factory<Route>

    public static final int BUS = 0;

    public String route_id;
    public Agency agency;
    public String route_short_name;
    public String route_long_name;
    public String route_desc;
    public int    route_type;
    public URL    route_url;
    public String route_color;
    public String route_text_color;

    public static class Loader extends Entity.Loader<Route> {

        public Loader(GTFSFeed feed) {
            super(feed, "routes");
        }

        @Override
        public void loadOneRow() throws IOException {
            Route r = new Route();
            r.route_id = getStringField("route_id", true);
            r.agency = getRefField("agency_id", false, feed.agency); // TODO automatically associate with agency when there is only one agency in the feed
            r.route_short_name = getStringField("route_short_name", false); // one or the other required, needs a special avalidator
            r.route_long_name = getStringField("route_long_name", false);
            r.route_desc = getStringField("route_desc", false);
            r.route_type = getIntField("route_type", true, 0, 7);
            r.route_url = getUrlField("route_url", false);
            r.route_color = getStringField("route_color", false);
            r.route_text_color = getStringField("route_text_color", false);
            r.feed = feed;
            feed.routes.put(r.route_id, r);
        }

    }

}
