package org.opentripplanner.updater.stoptime;

import org.opentripplanner.routing.trippattern.strategy.ITripTimesUpdater;
import org.opentripplanner.updater.GraphUpdater;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 10/09/13
 * Time: 07:27
 * To change this template use File | Settings | File Templates.
 */
public interface StoptimeGraphUpdater extends GraphUpdater {

    /**
     * @return an updater for the trip times, this allows support differences update strategy
     */
    public ITripTimesUpdater getTripTimesUpdater();
}
