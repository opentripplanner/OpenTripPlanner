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
import java.util.Iterator;

public class Agency extends Entity {

    public String agency_id;
    public String agency_name;
    public URL    agency_url;
    public String agency_timezone;
    public String agency_lang;
    public String agency_phone;
    public URL    agency_fare_url;

    public static class Loader extends Entity.Loader<Agency> {

        public Loader(GTFSFeed feed) {
            super(feed, "agency");
        }

        @Override
        public void loadOneRow() throws IOException {
            Agency a = new Agency();
            a.agency_id    = getStringField("agency_id", false); // can only be absent if there is a single agency -- requires a special validator.
            a.agency_name  = getStringField("agency_name", true);
            a.agency_url   = getUrlField("agency_url", true);
            a.agency_lang  = getStringField("agency_lang", false);
            a.agency_phone = getStringField("agency_phone", false);
            a.agency_timezone = getStringField("agency_timezone", true);
            a.agency_fare_url = getUrlField("agency_fare_url", false);
            a.feed = feed;
            feed.agency.put(a.agency_id, a);
        }

    }

    public static class Writer extends Entity.Writer<Agency> {
        public Writer(GTFSFeed feed) {
            super(feed, "agency");
        }

        @Override
        public void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"agency_id", "agency_name", "agency_url", "agency_lang",
                    "agency_phone", "agency_timezone", "agency_fare_url"});
        }

        @Override
        public void writeOneRow(Agency a) throws IOException {
            writeStringField(a.agency_id);
            writeStringField(a.agency_name);
            writeUrlField(a.agency_url);
            writeStringField(a.agency_lang);
            writeStringField(a.agency_phone);
            writeStringField(a.agency_timezone);
            writeUrlField(a.agency_fare_url);
            endRecord();
        }

        @Override
        public Iterator<Agency> iterator() {
            return this.feed.agency.values().iterator();
        }
    }

}
