package org.opentripplanner.analyst.scenario;

import com.conveyal.gtfs.model.Route;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import org.apache.commons.math3.analysis.function.Add;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.model.json_serialization.*;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/** Add a trip pattern */
public class AddTripPattern extends Modification {
    public static final long serialVersionUID = 1L;
    public static final Logger LOG = LoggerFactory.getLogger(AddTripPattern.class);

    /** The name of this pattern */
    public String name;

    /** The geometry of this pattern */
    @JsonDeserialize(using = EncodedPolylineJSONDeserializer.class)
    @JsonSerialize(using = EncodedPolylineJSONSerializer.class)
    public LineString geometry;

    /** What coordinate indices should be stops */
    @JsonDeserialize(using = BitSetDeserializer.class)
    @JsonSerialize(using = BitSetSerializer.class)
    public BitSet stops;

    /** The timetables for this trip pattern */
    public Collection<PatternTimetable> timetables;

    /** used to store the indices of the temporary stops in the graph */
    public transient TemporaryStop[] temporaryStops;

    /** GTFS mode (route_type), see constants in com.conveyal.gtfs.model.Route */
    public int mode = Route.BUS;

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
    public static class PatternTimetable implements Serializable {
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

        /** What days is this active on (starting with Monday at 0)? */
        @JsonDeserialize(using = BitSetDeserializer.class)
        @JsonSerialize(using = BitSetSerializer.class)
        public BitSet days;
    }

    /** A class representing a stop temporarily in the graph */
    public static class TemporaryStop {
        /** The indices of temporary stops are negative numbers to avoid clashes with the positive (vertex) indices of permanent stops. Note that code in RaptorWorkerData depends on these being negative. */
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

            if (this.sample == null)
                LOG.warn("Temporary stop unlinked: {}", this);
        }

        public String toString () {
            return "Temporary stop at " + this.lat + ", " + this.lon;
        }
    }
}
