package org.opentripplanner.routing.impl.raptor;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;

public class RaptorBoardSpec {
    int departureTime; // this gets put in the arrival time field, but board states are not real
                       // states so that is OK

    TripTimes tripTimes;
    int patternIndex;
    ServiceDay serviceDay;

    AgencyAndId tripId;

}
