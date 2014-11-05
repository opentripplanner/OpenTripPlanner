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

public class FareRule extends Entity {

    public String fare_id;
    public String route_id;
    public String origin_id;
    public String destination_id;
    public String contains_id;

    public static class Loader extends Entity.Loader<FareRule> {

        public Loader(GTFSFeed feed) {
            super(feed, "fare_rules");
        }

        @Override
        public void loadOneRow() throws IOException {
            FareRule fr = new FareRule();
            fr.fare_id = getStringField("fare_id", true);
            fr.route_id = getStringField("route_id", false);
            fr.origin_id = getStringField("origin_id", false);
            fr.destination_id = getStringField("destination_id", false);
            fr.contains_id = getStringField("contains_id", false);

            /* Check referential integrity. */
            getRefField("fare_id", true, feed.fareAttributes); // fare_rules add information to existing fare_attributes

            feed.fareRules.put(fr.fare_id, fr);
        }

    }

}
