package org.opentripplanner.common.geometry;

import java.util.Collection;

import org.opensphere.geometry.algorithm.ConcaveHull;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GraphUtils {

    public static Geometry makeConcaveHull(Graph graph) {
        GeometryFactory gf = GeometryUtils.getGeometryFactory();
        Collection<Vertex> vertices = graph.getVertices();
        Geometry[] points = new Geometry[vertices.size()];
        int i = 0;
        for (Vertex v : vertices) {
            points[i++] = gf.createPoint(v.getCoordinate());
        }

        GeometryCollection geometries = new GeometryCollection(points, gf);
        ConcaveHull hull = new ConcaveHull(geometries, 0.01);
        return hull.getConcaveHull();
    }

}
