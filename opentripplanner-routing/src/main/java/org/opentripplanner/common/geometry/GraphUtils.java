package org.opentripplanner.common.geometry;

import java.util.Collection;

import org.opensphere.geometry.algorithm.ConcaveHull;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class GraphUtils {

    public static Geometry makeConcaveHull(Graph graph) {
        GeometryCollection geometries = geometryCollectionFromVertices(graph);
        ConcaveHull hull = new ConcaveHull(geometries, 0.01);
        return hull.getConcaveHull();
    }

    private static GeometryCollection geometryCollectionFromVertices(Graph graph) {
        GeometryFactory gf = GeometryUtils.getGeometryFactory();
        Collection<Vertex> vertices = graph.getVertices();
        Geometry[] points = new Geometry[vertices.size()];
        int i = 0;
        for (Vertex v : vertices) {
            points[i++] = gf.createPoint(v.getCoordinate());
        }

        GeometryCollection geometries = new GeometryCollection(points, gf);
        return geometries;
    }

    public static Geometry makeConvexHull(Graph graph) {
        return new ConvexHull(geometryCollectionFromVertices(graph)).getConvexHull();
    }

    public static Geometry makeBuffer(Graph graph) {
        Geometry geom = geometryCollectionFromVertices(graph).buffer(.04, 6);

        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(geom);
        simplifier.setDistanceTolerance(0.00001);

        return simplifier.getResultGeometry();
    }

}
