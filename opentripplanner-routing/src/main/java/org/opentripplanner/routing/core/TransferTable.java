/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.P2;

/**
 * This class represents all transfer information in the graph. Transfers are grouped
 * by stop-to-stop pairs. Each transfer may consist of multiple specific transfers. 
 * See https://developers.google.com/transit/gtfs/reference#transfers_fields
 * and https://support.google.com/transitpartners/answer/2450962 (heading Route-to-route
 * and trip-to-trip transfers) for more details about the specifications.
 * @see StopTransfer, SpecificTransfer
 */
public class TransferTable implements Serializable {

    private static final long serialVersionUID = 9160765220742241406L;
    
    /**
     * Table which contains transfers between two stops
     */
    protected HashMap<P2<Stop>, StopTransfer> table = new HashMap<P2<Stop>, StopTransfer>();
    
    /**
     * Indicates that preferred transfers are present if true
     */
    protected boolean preferredTransfers = false;
    
    public boolean hasPreferredTransfers() {
        return preferredTransfers;
    }
    
    /**
     * Get the transfer time that should be used when transferring from a trip to another trip.
     * Note that this function does not check whether another specific transfer exists with the
     * same specificity, what is forbidden by the specifications.    
     * @param fromStop is the arriving stop
     * @param toStop is the departing stop
     * @param fromTrip is the arriving trip
     * @param toTrip is the departing trip
     * @return the transfer time in seconds. May contain special (negative) values which meaning
     *   can be found in the StopTransfer.*_TRANSFER constants.
     */
    public int getTransferTime(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip) {
        // Define transfer time to return
        int transferTime = StopTransfer.UNKNOWN_TRANSFER; 
        // Lookup transfer between two stops
        StopTransfer stopTransfer = table.get(new P2<Stop>(fromStop, toStop));
        if (stopTransfer != null) {
            // Lookup correct transfer time between two stops and two trips
            transferTime = stopTransfer.getTransferTime(fromTrip, toTrip);
        }
        return transferTime;
    }
    
    /**
     * Add a transfer time to the transfer table.
     * @param fromStop is the arriving stop
     * @param toStop is the departing stop
     * @param fromRoute is the arriving route; is allowed to be null
     * @param toRoute is the departing route; is allowed to be null
     * @param fromTrip is the arriving trip; is allowed to be null
     * @param toTrip is the departing trip; is allowed to be null
     * @param transferTime is the transfer time in seconds. May contain special (negative) values
     *   which meaning can be found in the StopTransfer.*_TRANSFER constants.
     */
    public void addTransferTime(Stop fromStop, Stop toStop, Route fromRoute, Route toRoute, Trip fromTrip, Trip toTrip, int transferTime) {
        checkNotNull(fromStop);
        checkNotNull(toStop);

        if (transferTime == StopTransfer.PREFERRED_TRANSFER) {
            preferredTransfers = true;
        }
        
        // Lookup whether a transfer between the two stops already exists
        P2<Stop> stopPair = new P2<Stop>(fromStop, toStop);
        StopTransfer stopTransfer = table.get(stopPair);
        if (stopTransfer == null) {
            // If not, create one and add to table
            stopTransfer = new StopTransfer();
            table.put(stopPair, stopTransfer);
        }
        assert(stopTransfer != null);
        
        // Create a specific transfer to add to the stop transfer
        SpecificTransfer specificTransfer;
        // Prepare the fields
        AgencyAndId fromRouteId = null;
        if (fromRoute != null) {
            fromRouteId = fromRoute.getId();
        }
        AgencyAndId toRouteId = null;
        if (toRoute != null) {
            toRouteId = toRoute.getId();
        }
        AgencyAndId fromTripId = null;
        if (fromTrip != null) {
            fromTripId = fromTrip.getId();
        }
        AgencyAndId toTripId = null;
        if (toTrip != null) {
            toTripId = toTrip.getId();
        }
        
        // Create and add the specific transfer to the stop transfer
        specificTransfer = new SpecificTransfer(fromRouteId, toRouteId, fromTripId, toTripId, transferTime);
        stopTransfer.addSpecificTransfer(specificTransfer);
    }
    
    /**
     * Internal class for testing purposes only.
     * @see TransferGraphLinker
     */
    public static class Transfer {
        public Stop from, to;
        public int seconds;
        public Transfer(Stop from, Stop to, int seconds) {
            this.from = from;
            this.to = to;
            this.seconds = seconds;
        }
    }
    
    /**
     * Public function for testing purposes only.
     * Returns only the transfers that contain a specific transfer with no fromRoute, toRoute,
     * fromTrip or toTrip defined.
     * @see TransferGraphLinker
     */
    public Iterable<Transfer> getAllTransfers() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>(table.size());
        for (Entry<P2<Stop>, StopTransfer> entry : table.entrySet()) {
            P2<Stop> p2 = entry.getKey();
            int transferTime = entry.getValue().getUnspecificTransferTime();
            if (transferTime != StopTransfer.UNKNOWN_TRANSFER) {
                transfers.add(new Transfer(p2.getFirst(), p2.getSecond(), transferTime));
            }
        }
        return transfers;
    }

    /**
     * Public function for testing purposes only.
     * Get the transfer time between two stops. Only works when a specific transfer with no fromRoute,
     * toRoute, fromTrip or toTrip defined exists.    
     * @param fromStop is the arriving stop
     * @param toStop is the departing stop
     * @return the transfer time in seconds. May contain special (negative) values which meaning
     *   can be found in the StopTransfer.*_TRANSFER constants.
     */
    public int getUnspecificTransferTime(Stop fromStop, Stop toStop) {
        // Define transfer time to return
        int transferTime = StopTransfer.UNKNOWN_TRANSFER; 
        // Lookup transfer between two stops
        StopTransfer stopTransfer = table.get(new P2<Stop>(fromStop, toStop));
        if (stopTransfer != null) {
            // Lookup correct transfer time between two stops and two trips
            transferTime = stopTransfer.getUnspecificTransferTime();
        }
        return transferTime;
    }
}
