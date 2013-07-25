/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.stoptime;

import java.util.List;
import javax.annotation.PostConstruct;

import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.edgetype.TimetableSnapshotSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.TripUpdate;
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
     * snapshot, just return the same one. Throttles the potentially resource-consuming task of 
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
    
    /** Should expired realtime data be purged from the graph. */
    @Setter private boolean purgeExpiredData = true;
    
    /** The TransitIndexService */
    private TransitIndexService transitIndexService;
    
    
    private ServiceDate lastPurgeDate = new ServiceDate();
    
    // nothing in the timetable snapshot binds it to one graph. we could use this updater for all
    // graphs at once
    private Graph graph;
    private long lastSnapshotTime = -1;
    
    /**
     * Set the data sources for the target graphs.
     */
    @PostConstruct
    public void setup () {
        graph = graphService.getGraph();
        graph.timetableSnapshotSource = this;
        transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null)
            throw new RuntimeException(
                    "Real-time update need a TransitIndexService. Please setup one during graph building.");
    }
    
    public TimetableResolver getSnapshot() {
        return getSnapshot(false);
    }
    
    private synchronized TimetableResolver getSnapshot(boolean force) {
        long now = System.currentTimeMillis();
        if (force || now - lastSnapshotTime > maxSnapshotFrequency) {
            if (force || buffer.isDirty()) {
                LOG.debug("Committing {}", buffer.toString());
                snapshot = buffer.commit();
            } else {
                LOG.debug("Buffer was unchanged, keeping old snapshot.");
            }
            lastSnapshotTime = System.currentTimeMillis();
        } else {
            LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot);
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
            List<TripUpdate> tripUpdates = updateStreamer.getUpdates(); 
            if (tripUpdates == null) {
                LOG.debug("tripUpdates is null");
                continue;
            }

            LOG.debug("message contains {} trip update blocks", tripUpdates.size());
            int uIndex = 0;
            for (TripUpdate tripUpdate : tripUpdates) {
                uIndex += 1;
                LOG.debug("trip update block #{} ({} updates) :", uIndex, tripUpdate.getUpdates().size());
                LOG.trace("{}", tripUpdate.toString());
                
                boolean applied = false;
                switch(tripUpdate.getStatus()) {
                case ADDED:
                    applied = handleAddedTrip(tripUpdate);
                    break;
                case CANCELED:
                    applied = handleCanceledTrip(tripUpdate);
                    break;
                case MODIFIED:
                    applied = handleModifiedTrip(tripUpdate);
                    break;
                case REMOVED:
                    applied = handleRemovedTrip(tripUpdate);
                    break;
                }
                
                if(applied) {
                   appliedBlockCount++;
                } else {
                    LOG.warn("Failed to apply Tripupdate: " + tripUpdate);
                }

                if (appliedBlockCount % logFrequency == 0) {
                    LOG.info("applied {} stoptime update blocks.", appliedBlockCount);
                }
            }

            LOG.debug("end of update message");

            // Make a snapshot after each message in anticipation of incoming requests
            // Purge data if necessary (and force new snapshot if anything was purged)
            if(purgeExpiredData) {
                boolean modified = purgeExpiredData(); 
                getSnapshot(modified);
            }
            else {
                getSnapshot(); 
            }
        }
    }

    protected boolean handleAddedTrip(TripUpdate tripUpdate) {
        // TODO: Handle added trip
        
        return false;
    }

    protected boolean handleCanceledTrip(TripUpdate tripUpdate) {

        TableTripPattern pattern = getPatternForTrip(tripUpdate.getTripId());
        if (pattern == null) {
            LOG.debug("No pattern found for tripId {}, skipping UpdateBlock.", tripUpdate.getTripId());
            return false;
        }

        boolean applied = buffer.update(pattern, tripUpdate);
        
        return applied;
    }

    protected boolean handleModifiedTrip(TripUpdate tripUpdate) {

        tripUpdate.filter(true, true, true);
        if (! tripUpdate.isCoherent()) {
            LOG.warn("Incoherent UpdateBlock, skipping.");
            return false;
        }
        if (tripUpdate.getUpdates().size() < 1) {
            LOG.debug("UpdateBlock contains no updates after filtering, skipping.");
            return false;
        }
        TableTripPattern pattern = getPatternForTrip(tripUpdate.getTripId());
        if (pattern == null) {
            LOG.debug("No pattern found for tripId {}, skipping UpdateBlock.", tripUpdate.getTripId());
            return false;
        }

        // we have a message we actually want to apply
        boolean applied = buffer.update(pattern, tripUpdate);
        
        return applied;
    }

    protected boolean handleRemovedTrip(TripUpdate tripUpdate) {
        // TODO: Handle removed trip
        
        return false;
    }
    
    protected TableTripPattern getPatternForTrip(AgencyAndId tripId) {
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        return pattern;
    }
    
    public String toString() {
        String s = (updateStreamer == null) ? "NONE" : updateStreamer.toString();
        return "Streaming stoptime updater with update streamer = " + s;
    }

    private boolean purgeExpiredData() {
        ServiceDate today = new ServiceDate();
        ServiceDate previously = today.previous().previous(); // Just to be safe... 
        
        if(lastPurgeDate.compareTo(previously) > 0) {
            return false;
        }
        
        LOG.debug("purging expired realtime data");
        // TODO: purge expired realtime data
        
        lastPurgeDate = previously;
        
        return buffer.purgeExpiredData(previously);
    }
}
