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
import com.conveyal.gtfs.error.DuplicateKeyError;

import java.io.IOException;

public class FareAttribute extends Entity {

    public String fare_id;
    public double price;
    public String currency_type;
    public int payment_method;
    public int transfers;
    public int transfer_duration;

    public static class Loader extends Entity.Loader<FareAttribute> {

        public Loader(GTFSFeed feed) {
            super(feed, "fare_attributes");
        }

        @Override
        public void loadOneRow() throws IOException {

            /* Calendars and Fares are special: they are stored as joined tables rather than simple maps. */
            String fareId = getStringField("fare_id", true);
            Fare fare = feed.getOrCreateFare(fareId);
            if (fare.fare_attribute != null) {
                feed.errors.add(new DuplicateKeyError(tableName, row, "fare_id"));
            } else {
                FareAttribute fa = new FareAttribute();
                fa.fare_id = fareId;
                fa.price = getDoubleField("price", true, 0, Integer.MAX_VALUE);
                fa.currency_type = getStringField("currency_type", true);
                fa.payment_method = getIntField("payment_method", true, 0, 1);
                fa.transfers = getIntField("transfers", false, 0, 10); // TODO missing means "unlimited" in this case (rather than 0), supply default value or just use the NULL to mean unlimited
                fa.transfer_duration = getIntField("transfer_duration", false, 0, 24 * 60 * 60);
                fa.feed = feed;
                fare.fare_attribute = fa;
            }

        }

    }

}
