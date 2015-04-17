package org.opentripplanner.analyst.scenario;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;

import java.util.BitSet;

public class AddFrequencyRoute extends Modification {

    /** The average speed over the whole route in km/h, not including dwell time at stops. */
    public double averageSpeedKph;

    /** The time that a vehicle sits still after arriving at each stop. */
    public int dwellTimeSeconds;

    /** The time between successive arrivals or departures at a each stop. */
    public double headwayMinutes;

    /** The geometry of the route alignment in WGS84 coordinates. */
    private LineString alignment;

    /** A set of coordinate indexes in the alignment that are transit stops. */
    public BitSet stops = new BitSet();

    public String getCoordinates() {
        return "TEST";
    }

    /**
     * Specify the transit route as a String in the format "lat0,lon0;lat1,lon1;lat2,lon2..."
     * Each coordinate pair can be accompanied by a third boolean value that indicates whether the point is a transit
     * stop or not. If the third value is not given, it is assumed that the point is a transit stop.
     *
     * For example "1,1,true;1,2,false;1,3,false;1,4,false;1,5" defines five points,
     * the first and last of which are transit stops.
     */
    public void setTransitRoute (String coordinateSequence) {
        CoordinateArrayListSequence coords = new CoordinateArrayListSequence();
        int i = 0;
        for (String coord : coordinateSequence.split(";")) {
            String[] fields = coord.split(",");
            if (fields.length < 2) {
                warnings.add("coordinate has too few fields: " + coord);
                continue;
            }
            boolean isStop = true;
            if (fields.length > 2) {
                isStop = Boolean.parseBoolean(fields[2]);
            }
            double lat = Double.parseDouble(fields[0]);
            double lon = Double.parseDouble(fields[1]);
            coords.add(new Coordinate(lat, lon));
            if (isStop) {
                stops.set(i);
            }
            i++;
        }
        GeometryFactory gf = new GeometryFactory();
        this.alignment = gf.createLineString(coords);
    }

    // TODO boolean followStreets

    @Override
    public void applyToGraph() {

    }

}
