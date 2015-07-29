package org.opentripplanner.transit;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class TransitLayer implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayer.class);

    public transient List<String> stops = new ArrayList<>();

    public List<TripPattern> tripPatterns = new ArrayList<>();

    public void loadFromGtfs (GTFSFeed gtfs) {
        TObjectIntMap<String> indexForStopId = new TObjectIntHashMap<>();
        for (Stop stop : gtfs.stops.values()) {
            indexForStopId.put(stop.stop_id, stops.size());
            stops.add(stop.stop_id);
        }
        // Group trips by stop pattern (including pickup/dropoff type) and fill in trip times.
        LOG.info("Grouping trips by stop pattern and creating trip schedules.");
        Map<List<PickDropStop>, TripPattern> tripPatternForStopSequence = new HashMap<>();
        int nTripsAdded = 0;
        for (String tripId : gtfs.trips.keySet()) {
            Trip trip = gtfs.trips.get(tripId);
            // Construct the stop pattern and schedule for this trip
            List<PickDropStop> pickDropStops = new ArrayList<>(30);
            TIntList arrivals = new TIntArrayList(30);
            TIntList departures = new TIntArrayList(30);
            for (StopTime st : gtfs.getOrderedStopTimesForTrip(tripId)) {
                int stopIndex = indexForStopId.get(st.stop_id);
                pickDropStops.add(new PickDropStop(stopIndex, st.pickup_type, st.drop_off_type));
                arrivals.add(st.arrival_time);
                departures.add(st.departure_time);
            }
            TripPattern tripPattern = tripPatternForStopSequence.get(pickDropStops);
            if (tripPattern == null) {
                tripPattern = new TripPattern(pickDropStops);
                tripPatternForStopSequence.put(pickDropStops, tripPattern);
                tripPatterns.add(tripPattern);
            }
            tripPattern.addTrip(new TripSchedule(trip, arrivals.toArray(), departures.toArray()));
            nTripsAdded += 1;
        }
        LOG.info("Done creating {} trips on {} patterns.", nTripsAdded, tripPatternForStopSequence.size());
    }

    public static TransitLayer fromGtfs (String file) {
        GTFSFeed gtfs = GTFSFeed.fromFile(file);
        TransitLayer transitLayer = new TransitLayer();
        transitLayer.loadFromGtfs(gtfs);
        return transitLayer;
    }

    public static TransitLayer read (InputStream stream) throws Exception {
        LOG.info("Reading transit layer...");
        FSTObjectInput in = new FSTObjectInput(stream);
        TransitLayer result = (TransitLayer) in.readObject(TransitLayer.class);
        in.close();
        LOG.info("Done reading.");
        return result;
    }

    public void write (OutputStream stream) throws IOException {
        LOG.info("Writing transit layer...");
        FSTObjectOutput out = new FSTObjectOutput(stream);
        out.writeObject(this, TransitLayer.class );
        out.close();
        LOG.info("Done writing.");
    }


}
