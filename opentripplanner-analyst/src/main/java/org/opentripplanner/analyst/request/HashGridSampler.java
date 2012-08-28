package org.opentripplanner.analyst.request;

import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.common.geometry.HashGrid.RasterizedSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;

//@Component
public class HashGridSampler implements SampleSource {

    private static final Logger LOG = LoggerFactory.getLogger(HashGridSampler.class);
    private static final double SEARCH_RADIUS_M = 100;
    @Autowired
    private GraphService graphService;
    private HashGrid index;

    @PostConstruct
    public void initialize() {
        index = new HashGrid(50, 2000, 2000);
        Graph graph = graphService.getGraph();
        LOG.debug("Rasterizing streets into index...");
        for (StreetVertex vertex : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
            for (Edge e: vertex.getOutgoing()) {
                index.rasterize(e.getGeometry(), e);
            }
        }
        LOG.debug("Done rasterizing streets into index.");
        //System.out.println(index.densityMap());
    }
    
    @Override
    /** implements SampleSource interface */
    public Sample getSample(double lon, double lat) {
        Coordinate coord = new Coordinate(lon, lat);
//        Point p = geometryFactory.createPoint(c);
        
        // track best two turn vertices
        RasterizedSegment s0 = null;
        RasterizedSegment s1 = null;
        Coordinate c0 = null;
        Coordinate c1 = null;
        double d0 = Double.MAX_VALUE;
        double d1 = Double.MAX_VALUE;

        // query
        List<Object> os = index.query(lon, lat, SEARCH_RADIUS_M);
        // query always returns a (possibly empty) list, but never null.
        // find two closest among nearby geometries
        for (Object o : os) {
            RasterizedSegment s = (RasterizedSegment) o;
            // inspired by a private method buried deep in JTS DistanceOp 
            Coordinate c = s.closestPoint(coord);
            //double d = DistanceLibrary.fastDistance(c, coord);
            double d = c.distance(coord);
            if (d > SEARCH_RADIUS_M)
                continue;
            if (d < d1) {
                if (d < d0) {
                    s1 = s0;
                    c1 = c0;
                    d1 = d0;
                    s0 = s;
                    c0 = c;
                    d0 = d;
                } else {
                    s1 = s;
                    c1 = c;
                    d1 = d;
                }
            }
        }
        
        // if at least one segment was found make a sample
        if (s0 != null) { 
            int t0 = timeToVertex(s0, c0, d0);
            int t1 = timeToVertex(s1, c1, d1);
            Sample s = new Sample((Vertex)s0.payload, t0, (Vertex)s1.payload, t1);
            return s;
        }
        return null;
    }

    private static int timeToVertex(RasterizedSegment segment, Coordinate coordOnSegment, 
                                    double distToSegment) {
        if (segment == null)
            return -1;
        double distOnSegment = segment.p0.distance(coordOnSegment);
        double dist = distOnSegment + distToSegment + segment.distAlongLinestring;
        int t = (int) (dist / 1.33);
        return t;
    }

}
