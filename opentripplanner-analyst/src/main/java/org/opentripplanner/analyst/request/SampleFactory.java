package org.opentripplanner.analyst.request;

import java.util.List;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Edge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

@Component
public class SampleFactory implements SampleSource {

    private static DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    @Autowired
    private GeometryIndex index;

    private double searchRadiusM;
    private double searchRadiusLon;
    private double searchRadiusLat;    

    public SampleFactory() {
        this.setSearchRadiusM(1000);
    }
    
    public void setSearchRadiusM(double radiusMeters) {
        this.searchRadiusM = radiusMeters;
        this.searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
        this.searchRadiusLon = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
    }

    @Override
    /** implements SampleSource interface */
    public Sample getSample(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        Point p = GeometryUtils.getGeometryFactory().createPoint(c);
        
        // track best two turn vertices
        Edge e0 = null;
        Edge e1 = null;
        DistanceOp o0 = null;
        DistanceOp o1 = null;
        double d0 = Double.MAX_VALUE;
        double d1 = Double.MAX_VALUE;

        // query
        Envelope env = new Envelope(c);
        env.expandBy(searchRadiusLon, searchRadiusLat);
        @SuppressWarnings("unchecked")
        List<Edge> edges = (List<Edge>) index.queryPedestrian(env);
        // query always returns a (possibly empty) list, but never null
        
        // find two closest among nearby geometries
        for (Edge v : edges) {
            Geometry g = v.getGeometry();
            DistanceOp o = new DistanceOp(p, g);
            double d = o.distance();
            if (d > searchRadiusLon)
                continue;
            if (d < d1) {
                if (d < d0) {
                    e1 = e0;
                    o1 = o0;
                    d1 = d0;
                    e0 = v;
                    o0 = o;
                    d0 = d;
                } else {
                    e1 = v;
                    o1 = o;
                    d1 = d;
                }
            }
        }
        
        // if at least one vertex was found make a sample
        if (e0 != null) { 
            int t0 = timeToEdge(e0, o0);
            int t1 = timeToEdge(e1, o1);
            Sample s = new Sample(e0.getFromVertex(), t0, e0.getToVertex(), t1);
            return s;
        }
        return null;
    }

    private static int timeToEdge(Edge e, DistanceOp o) {
        if (e == null)
            return -1;
        GeometryLocation[] gl = o.nearestLocations();
        Geometry g = e.getGeometry();
        LocationIndexedLine lil = new LocationIndexedLine(g);
        LinearLocation ll = lil.indexOf(gl[1].getCoordinate());
        LineString beginning = (LineString) 
                lil.extractLine(lil.getStartIndex(), ll);
        // WRONG: using unprojected coordinates
        double lengthRatio = beginning.getLength() / g.getLength();
        double distOnStreet = e.getDistance() * lengthRatio;
        double distToStreet = distanceLibrary .fastDistance(
                gl[0].getCoordinate(), 
                gl[1].getCoordinate());
        double dist = distOnStreet + distToStreet;
        int t = (int) (dist / 1.33);
        return t;
    }

}
