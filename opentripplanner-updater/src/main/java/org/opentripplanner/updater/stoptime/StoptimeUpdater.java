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
    @Setter    private static int logFrequency = 2000;

    /** 
     * If a timetable snapshot is requested less than this number of milliseconds after the previous 
     * snapshot, just return the same one. Thottles the potentially resource-consuming task of 
     * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
     */
    @Setter private int maxSnapshotFrequency = 1000; // msec    

    /** 
     * The last committed snapshot that was handed off to a routing thread. This snapshot may be
     * given to more than one routing thread if the maximum snapshot frequency is exceeded. 
     */
    private TimetableResolver snapshot = null;
    
    /** The working copy of the timetable resolver. Should not be visible to routing threads. */
    private TimetableResolver buffer = new TimetableResolver();
    
    /** A map from Trip AgencyAndIds to the TripPatterns that contain them */
    private Map<AgencyAndId, TableTripPattern> patternIndex;
    // nothing in the timetable snapshot binds it to one graph. we could use this updater for all
    // graphs at once
    private Graph graph;
    private long lastSnapshotTime = -1;
    
    /**
     * Once the data sources and target graphs have been set, index all trip patterns on the 
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
    }
    
    public synchronized TimetableResolver getSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime > maxSnapshotFrequency) {
            if (buffer.isDirty()) {
                LOG.debug("Committing {}", buffer.toString());
                snapshot = buffer.commit();
            } else {
                LOG.debug("Buffer was unchanged, keeping old snapshot.");
            }
            lastSnapshotTime = System.currentTimeMillis();
        } else {
            LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot.toString());
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
                boolean applied = buffer.update(pattern, block);
                if (applied) {
                    appliedBlockCount += 1;
                    if (appliedBlockCount % logFrequency == 0) {
                        LOG.info("applied {} stoptime update blocks.", appliedBlockCount);
                    }
                    // consider making a snapshot immediately in anticipation of incoming requests 
                    getSnapshot(); 
                }
            }
            LOG.debug("end of update message");
        }
    }

    public String toString() {
        String s = (updateStreamer == null) ? "NONE" : updateStreamer.toString();
        return "Streaming stoptime updater with update streamer = " + s;
    }
    
}
