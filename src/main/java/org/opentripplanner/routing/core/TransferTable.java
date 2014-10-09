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
    protected HashMap<P2<AgencyAndId>, StopTransfer> table = new HashMap<P2<AgencyAndId>, StopTransfer>();
    
    /**
     * Preferred transfers (or timed transfers, which are preferred as well) are present if true
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
     * @param forwardInTime is true when moving forward in time; false when moving
     *   backwards in time (usually this will be the variable "boarding")
     * @return the transfer time in seconds. May contain special (negative) values which meaning
     *   can be found in the StopTransfer.*_TRANSFER constants. If no transfer is found,
     *   StopTransfer.UNKNOWN_TRANSFER is returned.
     */
    public int getTransferTime(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip, boolean forwardInTime) {
        checkNotNull(fromStop);
        checkNotNull(toStop);
        
        // Reverse from and to if we are moving backwards in time
        if (!forwardInTime) {
            Stop tempStop = fromStop;
            fromStop = toStop;
            toStop = tempStop;
            Trip tempTrip = fromTrip;
            fromTrip = toTrip;
            toTrip = tempTrip;
        }
        
        // Get transfer time between the two stops
        int transferTime = getTransferTime(fromStop.getId(), toStop.getId(), fromTrip, toTrip);
        
        // Check parents of stops if no transfer was found
        if (transferTime == StopTransfer.UNKNOWN_TRANSFER) {
            // Find parent ids
            AgencyAndId fromStopParentId = null;
            AgencyAndId toStopParentId = null;
            if (fromStop.getParentStation() != null 
                    && !fromStop.getParentStation().isEmpty()) {
                // From stop has a parent
                fromStopParentId = new AgencyAndId(fromStop.getId().getAgencyId(), fromStop.getParentStation());
            }
            if (toStop.getParentStation() != null 
                    && !toStop.getParentStation().isEmpty()) {
                // To stop has a parent
                toStopParentId = new AgencyAndId(toStop.getId().getAgencyId(), toStop.getParentStation());
            }
            
            // Check parent of from stop if no transfer was found
            if (fromStopParentId != null) {
                transferTime = getTransferTime(fromStopParentId, toStop.getId(), fromTrip, toTrip);
            }

            // Check parent of to stop if still no transfer was found
            if (transferTime == StopTransfer.UNKNOWN_TRANSFER
                    && toStopParentId != null) {
                transferTime = getTransferTime(fromStop.getId(), toStopParentId, fromTrip, toTrip);
            }

            // Check parents of both stops if still no transfer was found
            if (transferTime == StopTransfer.UNKNOWN_TRANSFER
                    && fromStopParentId != null
                    && toStopParentId != null) {
                transferTime = getTransferTime(fromStopParentId, toStopParentId, fromTrip, toTrip);
            }
        }
        
        return transferTime;
    }
    
    /**
     * Get the transfer time that should be used when transferring from a trip to another trip.
     * Note that this function does not check whether another specific transfer exists with the
     * same specificity, what is forbidden by the specifications.    
     * @param fromStopId is the id of the arriving stop
     * @param toStopId is the id of the departing stop
     * @param fromTrip is the arriving trip
     * @param toTrip is the departing trip
     * @return the transfer time in seconds. May contain special (negative) values which meaning
     *   can be found in the StopTransfer.*_TRANSFER constants. If no transfer is found,
     *   StopTransfer.UNKNOWN_TRANSFER is returned.
     */
    private int getTransferTime(AgencyAndId fromStopId, AgencyAndId toStopId, Trip fromTrip, Trip toTrip) {
        checkNotNull(fromStopId);
        checkNotNull(toStopId);
        
        // Define transfer time to return
        int transferTime = StopTransfer.UNKNOWN_TRANSFER; 
        // Lookup transfer between two stops
        StopTransfer stopTransfer = table.get(new P2<AgencyAndId>(fromStopId, toStopId));
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
     *   which meaning can be found in the StopTransfer.*_TRANSFER constants.  If no transfer is found,
     *   StopTransfer.UNKNOWN_TRANSFER is returned.
     */
    public void addTransferTime(Stop fromStop, Stop toStop, Route fromRoute, Route toRoute, Trip fromTrip, Trip toTrip, int transferTime) {
        checkNotNull(fromStop);
        checkNotNull(toStop);

        // Check whether this transfer is preferred (or timed)
        if (transferTime == StopTransfer.PREFERRED_TRANSFER
                || transferTime == StopTransfer.TIMED_TRANSFER) {
            preferredTransfers = true;
        }
        
        // Lookup whether a transfer between the two stops already exists
        P2<AgencyAndId> stopIdPair = new P2<AgencyAndId>(fromStop.getId(), toStop.getId());
        StopTransfer stopTransfer = table.get(stopIdPair);
        if (stopTransfer == null) {
            // If not, create one and add to table
            stopTransfer = new StopTransfer();
            table.put(stopIdPair, stopTransfer);
        }
        assert(stopTransfer != null);
        
        // Create and add a specific transfer to the stop transfer
        SpecificTransfer specificTransfer = new SpecificTransfer(fromRoute, toRoute, fromTrip, toTrip, transferTime);
        stopTransfer.addSpecificTransfer(specificTransfer);
    }
    
    /**
     * Determines the transfer penalty given a transfer time and a penalty for non-preferred
     * transfers. 
     * @param transferTime is the transfer time
     * @param nonpreferredTransferPenalty is the penalty for non-preferred transfers
     * @return the transfer penalty
     */
    public int determineTransferPenalty(int transferTime, int nonpreferredTransferPenalty) {
        int transferPenalty = 0;
        
        if (hasPreferredTransfers()) {
            // Only penalize transfers if there are some that will be depenalized
            transferPenalty = nonpreferredTransferPenalty;

            if (transferTime == StopTransfer.PREFERRED_TRANSFER
                    || transferTime == StopTransfer.TIMED_TRANSFER) {
                // Depenalize preferred transfers
                // Timed transfers are assumed to be preferred as well
                // TODO: verify correctness of this method (AMB)
                transferPenalty = 0;
            }
        }
        
        return transferPenalty;
    }
    
    /**
     * Internal class for testing purposes only.
     * @see TransferGraphLinker
     */
    @Deprecated
    public static class Transfer {
        public AgencyAndId fromStopId, toStopId;
        public int seconds;
        public Transfer(AgencyAndId fromStopId, AgencyAndId toStopId, int seconds) {
            this.fromStopId = fromStopId;
            this.toStopId = toStopId;
            this.seconds = seconds;
        }
    }
    
    /**
     * Public function for testing purposes only.
     * Returns only the first specific transfers.
     * @see TransferGraphLinker
     */
    @Deprecated
    public Iterable<Transfer> getAllFirstSpecificTransfers() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>(table.size());
        for (Entry<P2<AgencyAndId>, StopTransfer> entry : table.entrySet()) {
            P2<AgencyAndId> p2 = entry.getKey();
            int transferTime = entry.getValue().getFirstSpecificTransferTime();
            if (transferTime != StopTransfer.UNKNOWN_TRANSFER) {
                transfers.add(new Transfer(p2.first, p2.second, transferTime));
            }
        }
        return transfers;
    }
}
