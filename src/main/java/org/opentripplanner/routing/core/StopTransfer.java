package org.opentripplanner.routing.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.gtfs.model.Trip;

/**
 * StopTransfer class used by TransferTable. Represents a transfer between two stops.
 * Contains more specific transfer information depending on the routes and trips
 * that are involved.
 * See the links described at TransferTable for more details about the specifications.
 * @see TransferTable
 */
public class StopTransfer implements Serializable {

    private static final long serialVersionUID = -2669653246852789518L;

    /**
     * Special value for transferTime indicating that this transfer is unknown
     */
    public static final int UNKNOWN_TRANSFER = -999;

    /**
     * Special value for transferTime indicating that this transfer is preferred
     */
    public static final int PREFERRED_TRANSFER = -2;
    
    /**
     * Special value for transferTime indicating that this transfer is forbidden
     */
    public static final int FORBIDDEN_TRANSFER = -1;

    /**
     * Special value for transferTime indicating that this transfer is timed.
     * In a timed transfer, the departing vehicle will wait for passengers from the
     * arriving vehicle, so the minimum transfer time is effectively zero.
     */
    public static final int TIMED_TRANSFER = 0;
    
    /**
     * List with specific transfers for this transfer between two stops.
     */
    private final List<SpecificTransfer> specificTransfers = new LinkedList<SpecificTransfer>();
    
    /**
     * Default constructor
     */
    public StopTransfer() {
    }
    
    /**
     * Add a specific transfer to this transfer.
     * @param specificTransfer is the specific transfer; is not allowed to be null
     * @return true if successful
     */
    public boolean addSpecificTransfer(SpecificTransfer specificTransfer) {
        checkNotNull(specificTransfer);
        return specificTransfers.add(specificTransfer);
    }
    
    /**
     * Get the transfer time that should be used when transferring from a trip to another trip.
     * Note that this function does not check whether another specific transfer exists with the
     * same specificity, what is forbidden by the specifications.    
     * @param fromTrip is the arriving trip
     * @param toTrip is the departing trip
     * @return the transfer time in seconds. May contain special (negative) values which meaning
     *   can be found in the *_TRANSFER constants.
     */
    public int getTransferTime(Trip fromTrip, Trip toTrip) {
        // By default the transfer is unknown
        int transferTime = UNKNOWN_TRANSFER;
        
        // Pick the matching specific transfer with the highest specificity
        int maxFoundSpecificity = SpecificTransfer.MIN_SPECIFICITY - 1;
        for (SpecificTransfer specificTransfer : specificTransfers) {
            int specificity = specificTransfer.getSpecificity(); 
            if (specificity > maxFoundSpecificity) {
                if (specificTransfer.matches(fromTrip, toTrip)) {
                    // Set the found transfer time
                    transferTime = specificTransfer.transferTime;
                    maxFoundSpecificity = specificity;
                    
                    // Break when highest specificity is found
                    if (maxFoundSpecificity == SpecificTransfer.MAX_SPECIFICITY) {
                        break;
                    }
                }
            }
        }
        
        // Return transfer time
        return transferTime;
    }
    
    /**
     * Public function for testing purposes only.
     * @return the first specific transfer time
     * @see TransferTable
     */
    @Deprecated
    public int getFirstSpecificTransferTime() {
        // By default the transfer is unknown
        int transferTime = UNKNOWN_TRANSFER;
        
        // Pick the first specific transfer
        for (SpecificTransfer specificTransfer : specificTransfers) {
            // Set the found transfer time
            transferTime = specificTransfer.transferTime;
            break;
        }
        
        // Return transfer time
        return transferTime;
    }
}
