package org.opentripplanner.updater.stoptime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.trippattern.TripPattern;
import org.opentripplanner.routing.trippattern.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

import static org.opentripplanner.common.IterableLibrary.filter;

public class StoptimeUpdaterOld {

    @Autowired 
    private GraphService graphService;
    
    public static final int NEVER = Integer.MIN_VALUE;
    public static final int UPDATED = Integer.MAX_VALUE;
    
    private static class UpdatedTrip {
        TripPattern pattern;
        int tripIndex;
        int[] scheduledArrivals;
        int[] scheduledDepartures;
        int[] arrivals;
        int[] departures;
        int[] updateTimes; // 0 for never
        boolean dirty = false;
        
        public UpdatedTrip (AgencyAndId tripId) {
            TripPattern pattern = (TripPattern) patternForTripId.get(tripId);
            tripIndex = pattern.getTrips().indexOf(tripId);
            scheduledArrivals = pattern.getArrivals(tripIndex);
            scheduledDepartures = pattern.getDepartures(tripIndex);
            arrivals = scheduledArrivals;
            departures = scheduledDepartures;
            updateTimes = new int[scheduledDepartures.length];
            Arrays.fill(updateTimes, NEVER);
        }
    
        public synchronized void update(long time, int stopIndex, int arrival, int departure) {
            if ( ! dirty) {
                arrivals = arrivals.clone();
                departures = departures.clone();
                dirty = true;
            }
            arrivals[stopIndex] = arrival;
            departures[stopIndex] = departure;
            updateTimes[stopIndex] = UPDATED;
        }

        public synchronized boolean flush() {
            if (dirty) {
                interpolate();
                pattern.setArrivals(arrivals);
                pattern.setDepartures(departures);
                dirty = false;
                return true;
            }
            return false;
        }
        
    }
    
    private static Map<AgencyAndId, TripPattern> patternForTripId = new HashMap<AgencyAndId, TripPattern>();
    private static Map<AgencyAndId, UpdatedTrip> updatedTrips = new HashMap<AgencyAndId, UpdatedTrip>();
    
    @PostConstruct
    public void setup () {
        Graph g = graphService.getGraph();
        // index trip patterns on trip ids they contain
        for (TransitStopDepart tsd : filter(g.getVertices(), TransitStopDepart.class)) {
            for (PatternBoard pb : filter(tsd.getOutgoing(), PatternBoard.class)) {
                TripPattern pattern = pb.getPattern();
                for (Trip trip : pattern.getTrips()) {
                    patternForTripId.put(trip.getId(), pattern);
                }
            }
        }
    }
    
    public static void main(String[] params) {
        File file = new File("/var/otp/data/nl/gtfs-rt.protobuf");
        try {
            InputStream is = new FileInputStream(file);
            FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(is);
            System.out.println(feed);
            
            FeedHeader header = feed.getHeader();
            long timestamp = header.getTimestamp();

            for (FeedEntity entity : feed.getEntityList()) {
                System.out.println(entity);
//                TripUpdate tUpdate = entity.getTripUpdate();
//                String trip = tUpdate.getTrip().getTripId();
//                AgencyAndId tripId = new AgencyAndId("agency", trip);
//                UpdatedTrip uTrip = getOrMakeUpdatedTrip(tripId);
//                for (StopTimeUpdate sUpdate : tUpdate.getStopTimeUpdateList()) {
//                    uTrip.update(time, stopIndex, arrival, departure);
//                }
            }
            
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private static UpdatedTrip getOrMakeUpdatedTrip(AgencyAndId tripId) {
        UpdatedTrip ut = updatedTrips.get(tripId);
        if (ut == null) {
            ut = new UpdatedTrip(tripId);
            updatedTrips.put(tripId, ut);
        }
        return ut;
    }
}
