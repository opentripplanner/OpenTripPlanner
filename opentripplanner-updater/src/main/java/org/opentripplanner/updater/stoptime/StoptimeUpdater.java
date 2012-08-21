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
import org.opentripplanner.routing.edgetype.TimetableResolver;
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

    @Autowired private GraphService graphService;
    @Setter    private UpdateStreamer updateStreamer;
    
    /** 
     * If a timetable snapshot is requested less than this number of milliseconds after the previous 
     * snapshot, just return the same one. Thottles the potentially resource-consuming task of 
     * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
     */
    @Setter private int maxSnapshotFrequency = 500; // msec    

    /** 
     * The last committed snapshot that was handed off to a routing thread. This snapshot may be
     * given to more than one routing thread if the maximum snapshot frequency is exceeded. 
     */
    private TimetableResolver snapshot = null;
    
    /** The working copy of the timetable resolver. Should not be visible to routing threads. */
    private TimetableResolver buffer = new TimetableResolver();
    
    /** A map from Trip AgencyAndIds to the TripPatterns that contain them */
    private Map<AgencyAndId, TableTripPattern> patternIndex;
    // nothing in the timetable snapshot binds it to one graph. we can use this updater for all
    // graphs at once
    private Graph graph;
    private long lastSnapshotTime = -1;
    
    /**
     * Once the data sources and target graphs have been established, index all trip patterns on the 
     * tripIds of Trips they contain.
     */
    @PostConstruct
    public void setup () {
        graph = graphService.getGraph();
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
    
    /** 
     * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
     * timetable objects it references are guaranteed to never change, so the requesting thread is 
     * provided a consistent view of all TripTimes. The routing thread need only release its 
     * reference to the snapshot to release resources.
     */
    public synchronized TimetableResolver getSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime > maxSnapshotFrequency) {
            synchronized (buffer) {
                if (buffer.isDirty()) {
                    LOG.info("committing {}", buffer.toString());
                    buffer.commit();
                    snapshot = buffer;
                    buffer = buffer.mutableCopy();
                }
            }
            lastSnapshotTime = now;
        } else {
            LOG.info("Snapshot frequency exceeded. Reusing snapshot {}", snapshot.toString());
        }
        return snapshot;
    }
    
    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates,
     * and applies those updates to scheduled trips.
     */
    @Override
    public void run() {
        int appliedBlockCount = 0;
        while (true) {
            List<Update> updates = updateStreamer.getUpdates(); 
            if (updates == null) {
                LOG.debug("updates is null");
                continue;
            } 
            List<UpdateBlock> blocks = UpdateBlock.splitByTrip(updates);
            LOG.debug("message contains {} trip update blocks", blocks.size());
            int uIndex = 0;
            for (UpdateBlock block : blocks) {
                uIndex += 1;
                LOG.debug("update block #{} ({} updates) :", uIndex, block.updates.size());
                LOG.trace("{}", block.toString());
                block.filter(true, true, true);
                if (! block.isCoherent()) {
                    LOG.warn("Incoherent UpdateBlock, skipping.");
                    continue; 
                }
                if (block.updates.size() < 1) {
                    LOG.debug("UpdateBlock contains no updates after filtering, skipping.");
                    continue; 
                }
                TableTripPattern pattern = patternIndex.get(block.tripId);
                if (pattern == null) {
                    LOG.debug("No pattern found for tripId {}, skipping UpdateBlock.", block.tripId);
                    continue;
                }
                // we have a message we actually want to apply
                boolean applied = false;
                synchronized (buffer) {
                    // have update perform the clone, pull the update call out of sync block 
                    Timetable tt = buffer.modify(pattern);
                    applied = tt.update(block);
                }
                if (applied) {
                    appliedBlockCount += 1;
                    if (appliedBlockCount % 100 == 0) {
                        LOG.info("applied {} stoptime update blocks.", appliedBlockCount);
                    }
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
