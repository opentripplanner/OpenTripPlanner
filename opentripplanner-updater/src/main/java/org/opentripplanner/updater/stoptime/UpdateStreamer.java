package org.opentripplanner.updater.stoptime;

import org.opentripplanner.routing.trippattern.UpdateList;

public interface UpdateStreamer {

    public UpdateList getUpdates();
    
}
