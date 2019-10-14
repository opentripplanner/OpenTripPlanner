package org.opentripplanner.ext.siri.updater;

import uk.org.siri.siri20.Siri;

public interface EstimatedTimetableSource {
    /**
     * Wait for one message to arrive, and decode it into a List of TripUpdates. Blocking call.
     * @return a Siri-object potentially containing updates for several different trips,
     *         or null if an exception occurred while processing the message
     */
    Siri getUpdates();
    
    /**
     * @return true iff the last list with updates represent all updates that are active right
     *        now, i.e. all previous updates should be disregarded
     */
    boolean getFullDatasetValueOfLastUpdates();

    String getFeedId();
}
