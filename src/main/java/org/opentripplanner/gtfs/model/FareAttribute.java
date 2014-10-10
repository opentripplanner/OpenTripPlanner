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

public class FareAttribute {
    final public String fare_id;
    final public String price;
    final public String currency_type;
    final public String payment_method;
    final public String transfers;
    final public String transfer_duration;

    public FareAttribute(Map<String, String> row) {
        fare_id = row.get("fare_id");
        price = row.get("price");
        currency_type = row.get("currency_type");
        payment_method = row.get("payment_method");
        transfers = row.get("transfers");
        transfer_duration = row.get("transfer_duration");
    }
}
