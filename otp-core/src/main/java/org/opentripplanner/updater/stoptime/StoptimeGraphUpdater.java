package org.opentripplanner.updater.stoptime;

import org.opentripplanner.routing.trippattern.strategy.ITripTimesUpdater;
import org.opentripplanner.updater.GraphUpdater;

public interface StoptimeGraphUpdater extends GraphUpdater {

    /**
     * @return an updater for the trip times, this allows support differences update strategy
     */
    public ITripTimesUpdater getITripTimesUpdater();
}
