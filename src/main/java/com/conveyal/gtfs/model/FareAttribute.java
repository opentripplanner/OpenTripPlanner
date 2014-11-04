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
            requiredColumns = new String[] {"fare_id", "price", "transfers"}; // TODO this is kind of redundant
        }

        @Override
        public void loadOneRow() throws IOException {
            FareAttribute fa = new FareAttribute();
            fa.fare_id           = getStringField("fare_id", true);
            fa.price             = getDoubleField("price", true, 0, Integer.MAX_VALUE);
            fa.currency_type     = getStringField("currency_type", true);
            fa.payment_method    = getIntField("payment_method", true, 0, 1);
            fa.transfers         = getIntField("transfers", false, 0, 10); // TODO missing means "unlimited" in this case (rather than 0), supply default value
            fa.transfer_duration = getIntField("transfer_duration", false, 0, 24 * 60 * 60);

            feed.fareAttributes.put(fa.fare_id, fa);
        }

    }

}
