package org.opentripplanner.updater.stoptime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.trippattern.UpdateList;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.opentripplanner.common.IterableLibrary.filter;

/**
 * Update OTP stop time tables from some (realtime) source
 * @author abyrd
 */
@Component
public class StoptimeUpdater implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(StoptimeUpdater.class);

    public static final int NEVER = Integer.MIN_VALUE;
    public static final int UPDATED = Integer.MAX_VALUE;

    @Autowired
    private GraphService graphService;
    private UpdateStreamer updateStreamer;
    private Map<AgencyAndId, TableTripPattern> patternForTripId;
   
    @PostConstruct
    public void setup () {
        Graph g = graphService.getGraph();
        // index trip patterns on trip ids they contain
        patternForTripId = new HashMap<AgencyAndId, TableTripPattern>();
        for (TransitStopDepart tsd : filter(g.getVertices(), TransitStopDepart.class)) {
            for (TransitBoardAlight tba : filter(tsd.getOutgoing(), TransitBoardAlight.class)) {
                //
                if (!tba.isBoarding())
                    continue;
                    
                TableTripPattern pattern = tba.getPattern();
                for (Trip trip : pattern.getTrips()) {
                    patternForTripId.put(trip.getId(), pattern);
                }
            }
        }
//        System.out.println("Indexed trips:");
//        for (AgencyAndId tripId : patternForTripId.keySet()) {
//            System.out.println(tripId);
//        }
    }

    @Override
    public void run() {
        while (true) {
            // blocking call
            UpdateList updates = updateStreamer.getUpdates();
            // null return means exception while processing a message
            if (updates == null)
                continue;
            // an update list can contain updates for several trips. Split it into a list of 
            // updates for single trips, and handle each one separately.
            for (UpdateList ul : updates.splitByTrip()) {
                LOG.trace(ul.toString());
                if (! ul.isSane()) {
                    LOG.trace("incoherent stoptime UpdateList");
                    continue; 
                }
                TableTripPattern pattern = patternForTripId.get(ul.tripId);
                if (pattern != null) {
                    LOG.trace("pattern found for {}", ul.tripId);
                    pattern.update(ul);
                } else {
                    LOG.trace("pattern not found {}", ul.tripId);
                }
            }
        }
    }

    public void setUpdateStreamer (UpdateStreamer us) {
        this.updateStreamer = us;
    }

}
