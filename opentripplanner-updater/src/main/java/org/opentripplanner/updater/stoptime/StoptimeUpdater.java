package org.opentripplanner.updater.stoptime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.trippattern.TripPattern;
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
    private Map<AgencyAndId, TripPattern> patternForTripId;
   
    @PostConstruct
    public void setup () {
        Graph g = graphService.getGraph();
        // index trip patterns on trip ids they contain
        patternForTripId = new HashMap<AgencyAndId, TripPattern>();
        for (TransitStopDepart tsd : filter(g.getVertices(), TransitStopDepart.class)) {
            for (PatternBoard pb : filter(tsd.getOutgoing(), PatternBoard.class)) {
                TripPattern pattern = pb.getPattern();
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
            for (UpdateList ul : updateStreamer.getUpdates().splitByTrip()) {
                System.out.println(ul.toString());
                if (! ul.isSane()) {
                    LOG.debug("incoherent stoptime UpdateList");
                    continue; 
                }
                TripPattern pattern = patternForTripId.get(ul.tripId);
                if (pattern != null) {
                    LOG.debug("pattern found for {}", ul.tripId);
                    pattern.update(ul);
                } else {
                    LOG.debug("pattern not found {}", ul.tripId);
                }
            }
        }
    }

    public void setUpdateStreamer (UpdateStreamer us) {
        this.updateStreamer = us;
    }

}
