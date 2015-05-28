/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * This is a representation of a set of contiguous OSM areas, used for various tasks related to edge splitting, such as start/endpoint snapping and
 * adding new edges during transit linking.
 * 
 * @author novalis
 */
public class AreaEdgeList implements Serializable {
    private static final long serialVersionUID = 969137349467214074L;

    private ArrayList<AreaEdge> edges = new ArrayList<AreaEdge>();

    private HashSet<IntersectionVertex> vertices = new HashSet<IntersectionVertex>();

    // these are all of the original edges of the area, whether
    // or not there are corresponding OSM edges. It is used as part of a hack
    // to fix up areas after network linking.
    private Polygon originalEdges;

    private List<NamedArea> areas = new ArrayList<NamedArea>();

    public List<AreaEdge> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<AreaEdge> edges) {
        this.edges = edges;
        for (AreaEdge edge : edges) {
            vertices.add((IntersectionVertex) edge.getFromVertex());
        }
    }

    public void addEdge(AreaEdge edge) {
        edges.add(edge);
        vertices.add((IntersectionVertex) edge.getFromVertex());
    }

    public void removeEdge(AreaEdge edge) {
        edges.remove(edge);
        // reconstruct vertices
        vertices.clear();
        for (Edge e : edges) {
            vertices.add((IntersectionVertex) e.getFromVertex());
        }
    }

    /**
     * Safely add a vertex to this area. This creates edges to all other vertices unless those edges would cross one of the original edges.
     */
    public void addVertex(IntersectionVertex newVertex, Graph graph) {
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        if (edges.size() == 0) {
            throw new RuntimeException("Can't add a vertex to an empty area");
        }

        @SuppressWarnings("unchecked")
        HashSet<IntersectionVertex> verticesCopy = (HashSet<IntersectionVertex>) vertices.clone();
        VERTEX: for (IntersectionVertex v : verticesCopy) {
            LineString newGeometry = geometryFactory.createLineString(new Coordinate[] {
                    newVertex.getCoordinate(), v.getCoordinate() });

            // ensure that new edge does not leave the bounds of the original area, or
            // fall into any holes
            if (!originalEdges.union(originalEdges.getBoundary()).contains(newGeometry)) {
                continue VERTEX;
            }

            // check to see if this splits multiple NamedAreas. This code is rather similar to
            // code in OSMGBI, but the data structures are different

            createSegments(newVertex, v, areas, graph);
        }

        vertices.add(newVertex);
    }

    private void createSegments(IntersectionVertex from, IntersectionVertex to,
            List<NamedArea> areas, Graph graph) {

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
        if (intersects.size() == 1) {
            NamedArea area = intersects.get(0);

            double length = SphericalDistanceLibrary.distance(to.getCoordinate(), from.getCoordinate());

            AreaEdge forward = new AreaEdge(from, to, line, area.getRawName(), length,
                    area.getPermission(), false, this);
            forward.setStreetClass(area.getStreetClass());
            AreaEdge backward = new AreaEdge(to, from, (LineString) line.reverse(), area.getRawName(),
                    length, area.getPermission(), true, this);
            backward.setStreetClass(area.getStreetClass());
            edges.add(forward);
            edges.add(backward);

        } else {
            Coordinate startCoordinate = from.getCoordinate();
            Point startPoint = geometryFactory.createPoint(startCoordinate);
            for (NamedArea area : intersects) {
                Geometry polygon = area.getPolygon();
                if (!polygon.intersects(startPoint))
                    continue;
                Geometry lineParts = line.intersection(polygon);
                if (lineParts.getLength() > 0.000001) {
                    Coordinate edgeCoordinate = null;
                    // this is either a LineString or a MultiLineString (we hope)
                    if (lineParts instanceof MultiLineString) {
                        MultiLineString mls = (MultiLineString) lineParts;
                        for (int i = 0; i < mls.getNumGeometries(); ++i) {
                            LineString segment = (LineString) mls.getGeometryN(i);
                            if (segment.contains(startPoint)
                                    || segment.getBoundary().contains(startPoint)) {
                                edgeCoordinate = segment.getEndPoint().getCoordinate();
                            }
                        }
                    } else if (lineParts instanceof LineString) {
                        edgeCoordinate = ((LineString) lineParts).getEndPoint().getCoordinate();
                    } else {
                        continue;
                    }

                    String label = "area splitter at " + edgeCoordinate;
                    IntersectionVertex newEndpoint = (IntersectionVertex) graph.getVertex(label);
                    if (newEndpoint == null) {
                        newEndpoint = new IntersectionVertex(graph, label, edgeCoordinate.x,
                                edgeCoordinate.y);
                    }

                    createSegments(from, newEndpoint, Arrays.asList(area), graph);
                    createSegments(newEndpoint, to, intersects, graph);
                    break;
                }
            }
        }
    }

    public Polygon getOriginalEdges() {
        return originalEdges;
    }

    public void setOriginalEdges(Polygon polygon) {
        this.originalEdges = polygon;
    }

    public void addArea(NamedArea namedArea) {
        areas.add(namedArea);
    }

    public List<NamedArea> getAreas() {
        return areas;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        edges.trimToSize();
        out.defaultWriteObject();
    }
}
