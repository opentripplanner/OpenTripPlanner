package org.opentripplanner.routing.trippattern.strategy;

import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.trippattern.ScheduledTripTimes;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.trippattern.TripUpdateList;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 10/09/13
 * Time: 06:46
 * To change this template use File | Settings | File Templates.
 */
public interface ITripTimesUpdater {

    TripTimes updateTimes(ScheduledTripTimes scheduledTimes, TableTripPattern pattern ,TripUpdateList updateList);

}
