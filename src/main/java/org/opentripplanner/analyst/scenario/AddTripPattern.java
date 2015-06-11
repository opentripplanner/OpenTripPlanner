package org.opentripplanner.analyst.scenario;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.graph.Graph;

import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/** Add a trip pattern */
public class AddTripPattern extends Modification {
    /** The name of this pattern */
    public String name;

    /** The geometry of this pattern */
    public LineString geometry;

    /** What coordinate indices should be stops */
    public BitSet stops;

    /** The timetables for this trip pattern */
    public Collection<PatternTimetable> timetables;

    /** used to store the indices of the temporary stops in the graph */
    public transient TemporaryStop[] temporaryStops;

    /** Create temporary stops associated with the given graph. Note that a given AddTripPattern can be associated only with a single graph. */
    public void materialize (Graph graph) {
        SampleFactory sfac = graph.getSampleFactory();

        temporaryStops = new TemporaryStop[stops.cardinality()];

        int stop = 0;
        for (int i = stops.nextSetBit(0); i >= 0; i = stops.nextSetBit(i + 1)) {
            temporaryStops[stop++] = new TemporaryStop(geometry.getCoordinateN(i), sfac);
        }
    }

    @Override
    public String getType() {
        return "add-trip-pattern";
    }

    /** a class representing a minimal timetable */
    public static class PatternTimetable {
        /** hop times in seconds */
        public int[] hopTimes;

        /** dwell times in seconds */
        public int[] dwellTimes;

        /** is this a frequency entry? */
        public boolean frequency;

        /** start time (seconds since GTFS midnight) */
        public int startTime;

        /** end time for frequency-based trips (seconds since GTFS midnight) */
        public int endTime;

        /** headway for frequency-based patterns */
        public int headwaySecs;

        /** What days is this active on (starting with Monday at 0) */
        public BitSet days;
    }

    /** A class representing a stop temporarily in the graph */
    public static class TemporaryStop {
        /** The indices of temporary stops are negative numbers to avoid clashes with the positive indices of permanent stops */
        private static AtomicInteger nextId = new AtomicInteger(-1);

        /** the index of this stop in the graph */
        public final int index;

        /** The latitude of this stop */
        public final double lat;

        /** The longitude of this stop */
        public final double lon;

        /** how this vertex is connected to the graph */
        public final Sample sample;

        public TemporaryStop (Coordinate c, SampleFactory sampleFactory) {
            this(c.y, c.x, sampleFactory);
        }

        public TemporaryStop (double lat, double lon, SampleFactory sampleFactory) {
            this.lat = lat;
            this.lon = lon;
            this.index = nextId.decrementAndGet();
            this.sample = sampleFactory.getSample(this.lon, this.lat);
        }
    }
}
