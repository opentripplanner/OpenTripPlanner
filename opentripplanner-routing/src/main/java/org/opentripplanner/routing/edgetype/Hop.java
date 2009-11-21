package org.opentripplanner.routing.edgetype;

import java.util.ArrayList;
import java.util.Comparator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class Hop extends AbstractEdge implements Comparable<Hop>, Drawable, HoppableEdge {

    public static class HopArrivalTimeComparator implements Comparator<Hop> {

        public int compare(Hop arg0, Hop arg1) {
            int v1 = arg0.end.getArrivalTime();
            int v2 = arg1.end.getArrivalTime();
            return v1 - v2;
        }

    }

    private static final long serialVersionUID = -7761092317912812048L;

    private StopTime start;

    private StopTime end;

    private AgencyAndId _serviceId;

    private int elapsed;

    public AgencyAndId getServiceId() {
        return _serviceId;
    }

    public Hop(Vertex startJourney, Vertex endJourney, StopTime start, StopTime end)
            throws Exception {
        super(startJourney, endJourney);
        this.start = start;
        this.end = end;
        this._serviceId = start.getTrip().getServiceId();
        this.elapsed = end.getArrivalTime() - start.getDepartureTime();
    }

    public StopTime getStartStopTime() {
        return start;
    }

    public StopTime getEndStopTime() {
        return end;
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(elapsed);
        return new TraverseResult(elapsed, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-elapsed);
        return new TraverseResult(elapsed, state1);
    }

    public int compareTo(Hop arg0) {
        return this.end.compareTo(arg0.end);
    }

    public String toString() {
        return this.start + " " + this.end + " " + this._serviceId;
    }

    ArrayList<DrawablePoint> geometryCache = null;

    private Geometry geometry = null;

    public ArrayList<DrawablePoint> getDrawableGeometry() {
        if (geometryCache != null) {
            return geometryCache;
        }

        ArrayList<DrawablePoint> ret = new ArrayList<DrawablePoint>();

        ret.add(new DrawablePoint((float) this.start.getStop().getLon(), (float) this.start
                .getStop().getLat(), this.start.getDepartureTime()));
        ret.add(new DrawablePoint((float) this.end.getStop().getLon(), (float) this.end.getStop()
                .getLat(), this.end.getArrivalTime()));

        geometryCache = ret;
        return ret;
    }

    public String getDirection() {
        return start.getTrip().getTripHeadsign();
    }

    public double getDistance() {
        Stop stop1 = start.getStop();
        Stop stop2 = end.getStop();
        return DistanceLibrary.distance(stop1.getLat(), stop1.getLon(), stop2.getLat(), stop2.getLon());
    }

    public String getEnd() {
        return end.getStopHeadsign();
    }

    public TransportationMode getMode() {
        return GtfsLibrary.getTransportationMode(start.getTrip().getRoute());
    }

    public String getStart() {
        return start.getStopHeadsign();
    }

    public String getName() {
        return GtfsLibrary.getRouteName(start.getTrip().getRoute());
    }

    public Geometry getGeometry() {
        if (geometry == null) {

            GeometryFactory factory = new GeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING), 4326);
            Stop stop1 = start.getStop();
            Stop stop2 = end.getStop();

            Coordinate c1 = new Coordinate(stop1.getLon(), stop1.getLat());
            Coordinate c2 = new Coordinate(stop2.getLon(), stop2.getLat());

            geometry = factory.createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    public void setGeometry(Geometry line) {
        geometry = line;
    }

    /****
     * Private Methods
     ****/

}
