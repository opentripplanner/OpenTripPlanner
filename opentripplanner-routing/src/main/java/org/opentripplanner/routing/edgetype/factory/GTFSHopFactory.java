package org.opentripplanner.routing.edgetype.factory;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.edgetype.Hop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GTFSHopFactory {

    private GtfsRelationalDao _dao;

    public GTFSHopFactory(GtfsContext context) throws Exception {
        _dao = context.getDao();
    }

    public ArrayList<Hop> run(boolean verbose) throws Exception {

        ArrayList<Hop> ret = new ArrayList<Hop>();

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        int j = 0;
        int n = trips.size();
        for (Trip trip : trips) {
            j++;
            if (verbose && j % (n / 100) == 0) {
                System.out.println("Trip " + j + "/" + n);
            }
            List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTime st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                Hop hop = new Hop(null, null, st0, st1);
                ret.add(hop);
            }
        }

        return ret;
    }

    public ArrayList<Hop> run() throws Exception {
        return run(false);
    }
}
