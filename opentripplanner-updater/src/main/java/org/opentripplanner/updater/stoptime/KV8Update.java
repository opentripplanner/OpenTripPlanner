package org.opentripplanner.updater.stoptime;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.trippattern.Update;

public class KV8Update extends Update {

    private KV8Update(CTX ctx) {
        super(tripId, stopId, stopSeq, arrive, depart, status, timestamp);
    }

}
