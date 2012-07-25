package org.opentripplanner.routing.impl.raptor;

import org.opentripplanner.routing.core.ServiceDay;

public class RaptorBoardSpec {
    int departureTime; // this gets put in the arrival time field, but board states are not real
                       // states so that is OK

    int tripIndex;
    int patternIndex;
    ServiceDay serviceDay;
}
