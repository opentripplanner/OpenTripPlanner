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

import java.io.IOException;

public class FeedInfo extends Entity {

    public String feed_id = "NONE";
    public String feed_publisher_name;
    public String feed_publisher_url;
    public String feed_lang;
    public String feed_start_date;
    public String feed_end_date;
    public String feed_version;

    @Override
    public String getKey() {
        return null;
    }

    public static class Factory extends Entity.Factory<FeedInfo> {

        public Factory() {
            tableName = "feed_info";
            requiredColumns = new String[] { };
        }

        @Override
        public FeedInfo fromCsv() throws IOException {
            FeedInfo fi = new FeedInfo();
            fi.feed_id = getStringField("feed_id", false);
            fi.feed_publisher_name = getStringField("feed_publisher_name", true);
            fi.feed_publisher_url = getStringField("feed_publisher_url", true);
            fi.feed_lang = getStringField("feed_lang", true);
            fi.feed_start_date = getStringField("feed_start_date", false); // TODO getDateField
            fi.feed_end_date = getStringField("feed_end_date", false);
            fi.feed_version = getStringField("feed_version", false);
            // Note that like all other Entity subclasses, this also has a feedId field.
            return fi;
        }

    }

}
