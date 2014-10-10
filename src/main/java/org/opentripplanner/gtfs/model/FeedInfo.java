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

import java.util.Map;

public class FeedInfo {
    final public String feed_publisher_name;
    final public String feed_publisher_url;
    final public String feed_lang;
    final public String feed_start_date;
    final public String feed_end_date;
    final public String feed_version;

    public FeedInfo(Map<String, String> row) {
        feed_publisher_name = row.get("feed_publisher_name");
        feed_publisher_url = row.get("feed_publisher_url");
        feed_lang = row.get("feed_lang");
        feed_start_date = row.get("feed_start_date");
        feed_end_date = row.get("feed_end_date");
        feed_version = row.get("feed_version");
    }
}
