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

public class Agency extends Entity {

    public String agency_id;
    public String agency_name;
    public String agency_url;
    public String agency_timezone;
    public String agency_lang;
    public String agency_phone;
    public String agency_fare_url;

    @Override
    public String getKey() {
        return agency_id;
    }

    public static class Factory extends Entity.Factory<Agency> {

        public Factory() {
            tableName = "agency";
            requiredColumns = new String[] {"agency_name", "agency_timezone"};
        }

        @Override
        public Agency fromCsv() throws IOException {
            Agency a = new Agency();
            a.agency_id    = getStringField("agency_id", false); // can only be absent if there is a single agency -- requires a special validator.
            a.agency_name  = getStringField("agency_name", true);
            a.agency_url   = getStringField("agency_url", true);
            a.agency_lang  = getStringField("agency_lang", false);
            a.agency_phone = getStringField("agency_phone", false);
            a.agency_timezone = getStringField("agency_timezone", true);
            a.agency_fare_url = getStringField("agency_fare_url", false);
            return a;
        }

    }

}
