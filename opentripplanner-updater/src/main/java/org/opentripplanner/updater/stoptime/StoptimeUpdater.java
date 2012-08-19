package org.opentripplanner.updater.stoptime;

import static org.opentripplanner.common.IterableLibrary.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TableTripPattern.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TimetableSnapshotSource;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.UpdateBlock;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Update OTP stop time tables from some (realtime) source
 * @author abyrd
 */
public class StoptimeUpdater implements Runnable, TimetableSnapshotSource {

    private static final Logger LOG = LoggerFactory.getLogger(StoptimeUpdater.class);

    public static final int NEVER = Integer.MIN_VALUE;
    public static final int UPDATED = Integer.MAX_VALUE;

    @Autowired private GraphService graphService;
    @Setter    private UpdateStreamer updateStreamer;
    @Setter    private int maxSnapshotFrequency = 5000; // msec    
    /** 
     * The last committed snapshot that was handed off to a routing thread. This snapshot may be
     * given to more than one routing thread if the maximum snapshot frequency is exceeded. 
     */
    private TimetableSnapshot snapshot = null;
    /** The working copy of the timetable resolver. Should not be visible to routing threads. */
    private TimetableSnapshot buffer = new TimetableSnapshot();
    /** A map from Trip AgencyAndIds to the TripPatterns that contain them */
    private Map<AgencyAndId, TableTripPattern> patternIndex;
    // nothing in the timetable snapshot binds it to one graph. we can use this updater for all
    // graphs at once
    private Graph graph;
    private long lastSnapshotTime = -1;
    
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
        graph.timetableSnapshotSource = this;
        /*
        System.out.println("Indexed trips:");
        for (AgencyAndId tripId : patternForTripId.keySet()) {
            System.out.println(tripId);
        }
        */
    }
    
    public synchronized TimetableSnapshot getSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime > maxSnapshotFrequency) {
            synchronized (buffer) {
                if (buffer.isDirty()) {
                    LOG.debug("committing {}", buffer.toString());
                    buffer.commit();
                    snapshot = buffer;
                    buffer = buffer.mutableCopy();
                }
            }
            lastSnapshotTime = now;
        } else {
            LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot.toString());
        }
        return snapshot;
    }
    
    @Override
    public void run() {
        while (true) {
            List<Update> updates = updateStreamer.getUpdates(); 
            if (updates == null) {
                LOG.debug("updates is null");
                continue;
            } 
            List<UpdateBlock> blocks = UpdateBlock.splitByTrip(updates);
            LOG.debug("message contains {} trip update blocks", blocks.size());
            int uIndex = 0;
            for (UpdateBlock updateBlock : blocks) {
                uIndex += 1;
                LOG.debug("update block #{} :", uIndex);
                LOG.trace("{}", updateBlock.toString());
                updateBlock.filter(true, true, true);
                if (! updateBlock.isCoherent()) {
                    LOG.debug("incoherent stoptime UpdateList");
                    continue; 
                }
                TableTripPattern pattern = patternIndex.get(updateBlock.tripId);
                if (pattern == null) {
                    LOG.debug("pattern not found {}", updateBlock.tripId);
                    continue;
                }
                // we have a message we actually want to apply
                synchronized (buffer) {
                    Timetable tt = buffer.modify(pattern);
                    tt.update(updateBlock);
                }
            }
            LOG.debug("end of update message", uIndex);
        }
    }

    public String toString() {
        String s = (updateStreamer == null) ? "NONE" : updateStreamer.toString();
        return "Streaming stoptime updater with update streamer = " + s;
    }
    
}
