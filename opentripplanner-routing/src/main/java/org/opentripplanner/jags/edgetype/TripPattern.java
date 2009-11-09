package org.opentripplanner.jags.edgetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class TripPattern {
    /*
     * Represents a class of trips distinguished by service id, list of stops, dwell time at stops,
     * and running time between stops.
     */

    public Trip exemplar;

    private Vector<Integer> startTimes;

    private List<PatternStop> patternStops;

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
        patternStops = new ArrayList<PatternStop>();
        
        int startTime = stopTimes.get(0).getDepartureTime();
        
        for (StopTime stopTime : stopTimes) {
            PatternStop patternStop = new PatternStop(stopTime.getDepartureTime() - startTime,
                    stopTime.getArrivalTime() - startTime);
            patternStops.add(patternStop);
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

    public int getNextDepartureTime(int stopIndex, int afterTime) {

        PatternStop patternStop = patternStops.get(stopIndex);
        int departureTimeOffset = patternStop.departureTimeOffset;

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
