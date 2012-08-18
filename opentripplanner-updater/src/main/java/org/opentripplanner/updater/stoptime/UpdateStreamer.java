package org.opentripplanner.updater.stoptime;

import org.opentripplanner.routing.trippattern.UpdateList;

public interface UpdateStreamer {

    /**
     * Wait for one message to arrive, and decode it into an UpdateList. Blocking call. 
     * @return an UpdateList potentially containing updates for several different trips,
     *         or null if an exception occurred while processing the message
     */
    public UpdateList getUpdates();
    
}
