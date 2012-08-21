package org.opentripplanner.updater.stoptime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TableTripPattern.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
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
public class StoptimeUpdater implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(StoptimeUpdater.class);

    public static final int NEVER = Integer.MIN_VALUE;
    public static final int UPDATED = Integer.MAX_VALUE;

    @Autowired private GraphService graphService;
    @Setter    private UpdateStreamer updateStreamer;
    @Setter    private int maxSnapshotFrequency = 5000; // msec
    @Getter    private TimetableSnapshot snapshot = new TimetableSnapshot();
    private Map<AgencyAndId, TableTripPattern> patternIndex;
    private Graph graph;
    private long lastSnapshotTime = 0;
    
    @PostConstruct
    public void setup () {
        graph = graphService.getGraph();
        // index trip patterns on trip ids they contain
        patternIndex = new HashMap<AgencyAndId, TableTripPattern>();
        for (TransitStopDepart tsd : filter(graph.getVertices(), TransitStopDepart.class)) {
            for (TransitBoardAlight tba : filter(tsd.getOutgoing(), TransitBoardAlight.class)) {
                if (!tba.isBoarding())
                    continue;
                TableTripPattern pattern = tba.getPattern();
                for (Trip trip : pattern.getTrips()) {
                    patternIndex.put(trip.getId(), pattern);
                }
            }
        }
        /*
        System.out.println("Indexed trips:");
        for (AgencyAndId tripId : patternForTripId.keySet()) {
            System.out.println(tripId);
        }
        */
    }
    
    @Override
    public void run() {
        TimetableSnapshot buffer = snapshot;
        while (true) {
            // blocking call (must set timeout to correctly handle snapshot freq)
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
                TableTripPattern pattern = patternIndex.get(ul.tripId);
                if (pattern == null) {
                    LOG.trace("pattern not found {}", ul.tripId);
                    continue;
                }
                // we have a message we actually want to apply
                if (buffer == snapshot) {
                    buffer = snapshot.mutableCopy();
                }
                Timetable tt = buffer.modify(pattern);
                tt.update(ul);
            }
            /* 
             * To avoid concurrent read/writing of the services map in the graph, we could put the 
             * updater in it once and access the current snapshot via the updater, rather than 
             * repeatedly storing the current snapshot.
             * However, the StoptimeUpdater class is not visible from Routing.
             * So for now, I will stick the current snapshot directly into a field in Graph.
             */
            if (buffer != snapshot) {
                long now = System.currentTimeMillis();
                if (now - lastSnapshotTime > maxSnapshotFrequency) {
                    LOG.debug("committing {}", buffer.toString());
                    buffer.commit();
                    snapshot = buffer;
                    graph.timetableSnapshot = snapshot;
                    lastSnapshotTime = now;
                }
            }
        }
    }

    public String toString() {
        String s = (updateStreamer == null) ? "NONE" : updateStreamer.toString();
        return "Streaming stoptime updater with update streamer = " + s;
    }
    
}
