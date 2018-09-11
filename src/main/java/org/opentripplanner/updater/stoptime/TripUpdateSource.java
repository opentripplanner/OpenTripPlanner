package org.opentripplanner.updater.stoptime;

import java.util.List;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public interface TripUpdateSource {
    /**
     * Wait for one message to arrive, and decode it into a List of TripUpdates. Blocking call.
     * @return a List<TripUpdate> potentially containing TripUpdates for several different trips,
     *         or null if an exception occurred while processing the message
     */
    public List<TripUpdate> getUpdates();
    
    /**
     * @return true iff the last list with updates represent all updates that are active right
     *        now, i.e. all previous updates should be disregarded
     */
    public boolean getFullDatasetValueOfLastUpdates();

    public String getFeedId();
}
