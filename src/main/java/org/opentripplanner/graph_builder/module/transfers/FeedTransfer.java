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
package org.opentripplanner.graph_builder.module.transfers;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;

import java.io.Serializable;

@CsvFields(filename = "feed_transfers.csv")
public class FeedTransfer implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    @CsvField(name = "from_stop_id")
    private String fromStopId;

    @CsvField(name = "from_route_id", optional = true)
    private String fromRouteId;

    @CsvField(name = "from_trip_id", optional = true)
    private String fromTripId;

    @CsvField(name = "to_stop_id")
    private String toStopId;

    @CsvField(name = "to_route_id", optional = true)
    private String toRouteId;

    @CsvField(name = "to_trip_id", optional = true)
    private String toTripId;

    @CsvField(name = "transfer_type", optional = true)
    private int transferType;

    @CsvField(name = "min_transfer_time", optional = true)
    private int minTransferTime = MISSING_VALUE;

    @CsvField(name = "street_transfer", optional = true)
    private int streetTransfer;

    public FeedTransfer() {

    }

    public FeedTransfer(FeedTransfer obj) {
        this.fromStopId = obj.fromStopId;
        this.fromRouteId = obj.fromRouteId;
        this.fromTripId = obj.fromTripId;
        this.toStopId = obj.toStopId;
        this.toRouteId = obj.toRouteId;
        this.toTripId = obj.toTripId;
        this.transferType = obj.transferType;
        this.minTransferTime = obj.minTransferTime;
        this.streetTransfer = obj.streetTransfer;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public void setFromStopId(String fromStopId) {
        this.fromStopId = fromStopId;
    }

    public String getFromRouteId() {
        return fromRouteId;
    }

    public void setFromRouteId(String fromRouteId) {
        this.fromRouteId = fromRouteId;
    }

    public String getFromTripId() {
        return fromTripId;
    }

    public void setFromTripId(String fromTripId) {
        this.fromTripId = fromTripId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public void setToStopId(String toStopId) {
        this.toStopId = toStopId;
    }

    public String getToRouteId() {
        return toRouteId;
    }

    public void setToRouteId(String toRouteId) {
        this.toRouteId = toRouteId;
    }

    public String getToTripId() {
        return toTripId;
    }

    public void setToTripId(String toTripId) {
        this.toTripId = toTripId;
    }

    public int getTransferType() {
        return transferType;
    }

    public void setTransferType(int transferType) {
        this.transferType = transferType;
    }

    public boolean isMinTransferTimeSet() {
        return minTransferTime != MISSING_VALUE;
    }

    public int getMinTransferTime() {
        return minTransferTime;
    }

    public void setMinTransferTime(int minTransferTime) {
        this.minTransferTime = minTransferTime;
    }

    public void clearMinTransferTime() {
        this.minTransferTime = MISSING_VALUE;
    }

    public int getStreetTransfer() {
        return streetTransfer;
    }

    public void setStreetTransfer(int streetTransfer) {
        this.streetTransfer = streetTransfer;
    }

    public String toString() {
        return "<FeedTransfer>";
    }
}
