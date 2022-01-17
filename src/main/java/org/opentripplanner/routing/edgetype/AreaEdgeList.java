package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.locationtech.jts.operation.distance.DistanceOp;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * This is a representation of a set of contiguous OSM areas, used for various tasks related to edge splitting, such as start/endpoint snapping and
 * adding new edges during transit linking.
 * 
 * @author novalis
 */
public class AreaEdgeList implements Serializable {
    private static final long serialVersionUID = 969137349467214074L;

    public final HashSet<IntersectionVertex> visibilityVertices = new HashSet<>();

    // these are all of the original edges of the area, whether
    // or not there are corresponding OSM edges. It is used as part of a hack
    // to fix up areas after network linking.
    private final Polygon originalEdges;

    private final List<NamedArea> areas = new ArrayList<NamedArea>();

    public AreaEdgeList(Polygon originalEdges) {
        this.originalEdges = originalEdges;
    }

    /**
     * Safely add a vertex to this area. This creates edges to all other vertices unless those edges would cross one of the original edges.
     */
    public void addVertex(IntersectionVertex newVertex) {
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

        Geometry polygon = originalEdges.union(originalEdges.getBoundary()).buffer(0.000001);

        // Due to truncating of precision in storage of the edge geometry, the new split vertex
        // might be located just outside the area, so we calculate the point closest to the polygon
        // for the comparison.
        Coordinate[] nearestPoints = DistanceOp.nearestPoints(
            polygon,
            geometryFactory.createPoint(newVertex.getCoordinate())
        );

        int added = 0;

        for (IntersectionVertex v : visibilityVertices) {
            LineString newGeometry = geometryFactory.createLineString(
                new Coordinate[] {nearestPoints[0], v.getCoordinate() }
            );

            // ensure that new edge does not leave the bounds of the original area, or
            // fall into any holes
            if (!polygon.contains(newGeometry)) {
                continue;
            }

            // check to see if this splits multiple NamedAreas. This code is rather similar to
            // code in OSMGBI, but the data structures are different
            createSegments(newVertex, v, areas);
            added++;
        }

        // TODO: Temporary fix for unconnected area edges. This should go away when moving walkable
        // area calculation to be done after stop linking
        if (added == 0) {
            for (IntersectionVertex v : visibilityVertices) {
                createSegments(newVertex, v, areas);
            }
        }

        visibilityVertices.add(newVertex);
    }

    private void createSegments(IntersectionVertex from, IntersectionVertex to,
            List<NamedArea> areas) {

        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

        LineString line = geometryFactory.createLineString(new Coordinate[] { from.getCoordinate(),
                to.getCoordinate() });

        List<NamedArea> intersects = new ArrayList<NamedArea>();
        for (NamedArea area : areas) {
            Geometry polygon = area.getPolygon();
            Geometry intersection = polygon.intersection(line);
            if (intersection.getLength() > 0.000001) {
                intersects.add(area);
            }
        }
        if (intersects.size() > 0) {
            // If more than one area intersects, we pick one by random for the name & properties
            NamedArea area = intersects.get(0);

            double length = SphericalDistanceLibrary.distance(to.getCoordinate(), from.getCoordinate());

            AreaEdge forward = new AreaEdge(from, to, line, area.getRawName(), length,
                    area.getPermission(), false, this);
            forward.setStreetClass(area.getStreetClass());
            AreaEdge backward = new AreaEdge(to, from, (LineString) line.reverse(), area.getRawName(),
                    length, area.getPermission(), true, this);
            backward.setStreetClass(area.getStreetClass());
        }
    }

    public void addArea(NamedArea namedArea) {
        areas.add(namedArea);
    }

    public List<NamedArea> getAreas() {
        return areas;
    }
}
