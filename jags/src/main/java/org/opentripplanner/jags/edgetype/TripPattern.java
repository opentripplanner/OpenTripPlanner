package org.opentripplanner.jags.edgetype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class TripPattern {
    /*
     * Represents a class of trips distinguished by service id, list of stops, dwell time at stops,
     * and running time between stops.
     */

    public Trip exemplar;

    private Vector<Integer> startTimes;

    private Map<Stop, PatternStop> stops;

    class PatternStop {
        public int arrivalTimeOffset;

        public int departureTimeOffset;

        public PatternStop(int arrivalTimeOffset, int departureTimeOffset) {
            this.arrivalTimeOffset = arrivalTimeOffset;
            this.departureTimeOffset = departureTimeOffset;
        }
    }

    public TripPattern(Trip exemplar, List<StopTime> stopTimes) {
        this.exemplar = exemplar;
        startTimes = new Vector<Integer>();
        stops = new HashMap<Stop, PatternStop>();
        int startTime = stopTimes.get(0).getDepartureTime();
        
        for (StopTime stopTime : stopTimes) {
            PatternStop patternStop = new PatternStop(stopTime.getDepartureTime() - startTime,
                    stopTime.getArrivalTime() - startTime);
            stops.put(stopTime.getStop(), patternStop);
            
        }
    }

    public void addStartTime(int time) {
        int i;
        for (i = 0; i < startTimes.size(); ++i) {
            if (startTimes.elementAt(i) > time) {
                break;
            }
        }
        startTimes.insertElementAt(time, i);
    }

    public int getNextDepartureTime(Stop stop, int afterTime) {

        int departureTimeOffset = stops.get(stop).departureTimeOffset;

        for (int i = 0; i < startTimes.size(); ++i) {
            int startTime = startTimes.get(i);
            int departureTime = startTime + departureTimeOffset;
            if (departureTime >= afterTime) {
                return departureTime;
            }
        }
        return -1;
    }

}
