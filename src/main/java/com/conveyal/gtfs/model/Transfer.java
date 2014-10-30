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

public class Transfer extends Entity {

    public String from_stop_id;
    public String to_stop_id;
    public int    transfer_type;
    public int    min_transfer_time;

    @Override
    public String getKey() {
        return null;
    }

    public static class Factory extends Entity.Factory<Transfer> {

        public Factory() {
            tableName = "transfers";
            requiredColumns = new String[] {"from_stop_id", "to_stop_id", "transfer_type"};
        }

        @Override
        public Transfer fromCsv() throws IOException {
            Transfer tr = new Transfer();
            tr.from_stop_id      = getStringField("from_stop_id", true);
            tr.to_stop_id        = getStringField("to_stop_id", true);
            tr.transfer_type     = getIntField("transfer_type", true);
            tr.min_transfer_time = getIntField("min_transfer_time", false);
            return tr;
        }

    }

}
