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
import java.util.prefs.Preferences;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.updater.PreferencesConfigurable;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphUpdaterRunnable;
import org.opentripplanner.updater.RealtimeDataSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Update OTP stop time tables from some (realtime) source
 * @author abyrd
 */
public class StoptimeUpdater implements GraphUpdaterRunnable, GraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(StoptimeUpdater.class);

    private static final long DEFAULT_UPDATE_FREQ_SEC = 60;

    @Setter
    @Autowired private GraphService graphService;
    @Setter    private UpdateStreamer updateStreamer;
    @Setter    private int logFrequency = 2000;
    
    private int appliedBlockCount = 0;

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
    
    
    protected ServiceDate lastPurgeDate = null;
    
    protected Graph graph;
    protected long lastSnapshotTime = -1;
    
    public StoptimeUpdater() {
    }

    /**
     * Build a StoptimeUpdater binded to a single graph. Only used for tests.
     * @param graph
     */
    @Deprecated
    public StoptimeUpdater(Graph graph) {
        init(graph);
    }
    
    /**
     * Called when used in DI-context: graph is default one.
     */
    @PostConstruct
    public void init() {
        init(graphService.getGraph());
    }
    
    /**
     * Initialise for a given graph. Set the data sources for the target graphs.
     */
    private void init(Graph graph) {
        this.graph = graph;
//        graph.timetableSnapshotSource = this;
        transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null)
            throw new RuntimeException(
                    "Real-time update need a TransitIndexService. Please setup one during graph building.");
    }
    
    @Override
    public void setup() {
    }
    
    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates,
     * and applies those updates to the graph.
     */
    @Override
    public void run() {
        List<TripUpdateList> tripUpdateLists = updateStreamer.getUpdates();
        
        // Handle tripupdatelist
    }

    @Override
    public void teardown() {
    }

    protected TableTripPattern getPatternForTrip(AgencyAndId tripId) {
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        return pattern;
    }
    
    public String toString() {
        String s = (updateStreamer == null) ? "NONE" : updateStreamer.toString();
        return "Streaming stoptime updater with update streamer = " + s;
    }

    protected boolean purgeExpiredData() {
        ServiceDate today = new ServiceDate();
        ServiceDate previously = today.previous().previous(); // Just to be safe... 
        
        if(lastPurgeDate != null && lastPurgeDate.compareTo(previously) > 0) {
            return false;
        }
        
        LOG.debug("purging expired realtime data");
        // TODO: purge expired realtime data
        
        lastPurgeDate = previously;
        
        return buffer.purgeExpiredData(previously);
    }

    /**
     * Configure stop time updater
     * 
     * Usage example ('rt' name is an example):
     * 
     * <pre>
     * rt.type = stop-time-updater
     * rt.frequencySec = 60
     * rt.sourceType = gtfs-http
     * rt.url = http://host.tld/path
     * rt.defaultAgencyId = TA
     * </pre>
     * 
     */
    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        // Create update streamer from preferences
        String sourceType = preferences.get("sourceType", null);
        UpdateStreamer streamer = null;
        if (sourceType != null) {
            if (sourceType.equals("gtfs-http")) {
                streamer = new GtfsRealtimeHttpUpdateStreamer();
            }
            else if (sourceType.equals("gtfs-zmq")) {
                streamer = new GTFSZMQUpdateStreamer();
            }
            else if (sourceType.equals("kv8-zmq")) {
                streamer = new KV8ZMQUpdateStreamer();
            }
        }
        
        if (streamer == null) {
            throw new IllegalArgumentException("Unknown update streamer source type: " + sourceType);
        }
        else if (streamer instanceof PreferencesConfigurable) {
            ((PreferencesConfigurable) streamer).configure(graph, preferences);
        }
        
        // Configure updater
        init(graph);
        setUpdateStreamer(streamer);
        // Configure the updater itself
        int logFrequency = preferences.getInt("logFrequency", -1);
        if (logFrequency >= 0)
            setLogFrequency(logFrequency);
        int maxSnapshotFrequency = preferences.getInt("maxSnapshotFrequencyMs", -1);
        if (maxSnapshotFrequency >= 0)
            setMaxSnapshotFrequency(maxSnapshotFrequency);
        setPurgeExpiredData(preferences.getBoolean("purgeExpiredData", true));
        long frequencySec = preferences.getLong("frequencySec", DEFAULT_UPDATE_FREQ_SEC);
        LOG.info("Creating stop time updater running every {} seconds : {}", frequencySec,
                streamer);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        // TODO Auto-generated method stub
        
    }
}
