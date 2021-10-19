package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.NamedArea;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// TODO Temporary code until we refactor the WalkableAreaBuilder (#3152)
class AreaVisibilityAdjuster {
  private static final Logger LOG = LoggerFactory.getLogger(AreaVisibilityAdjuster.class);

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  private static final StreetEdgeFactory edgeFactory = new DefaultStreetEdgeFactory();

  // Link to all vertices in area/platform
  static void linkTransitToAreaVertices(Vertex splitterVertex, AreaEdgeList area) {
    List<Vertex> vertices = new ArrayList<>();

    Polygon originalEdges = area.getOriginalEdges();

    for (AreaEdge areaEdge : area.getEdges()) {
      if (!vertices.contains(areaEdge.getToVertex())) { vertices.add(areaEdge.getToVertex()); }
      if (!vertices.contains(areaEdge.getFromVertex())) { vertices.add(areaEdge.getFromVertex()); }
    }

    int  linkCount = 0;
    for (Vertex vertex : vertices) {
      if (vertex instanceof StreetVertex && !vertex.equals(splitterVertex)) {
        LineString line = GEOMETRY_FACTORY.createLineString(
            new Coordinate[] {
              splitterVertex.getCoordinate(),
              vertex.getCoordinate()
        });
        double length = SphericalDistanceLibrary.distance(
            splitterVertex.getCoordinate(),
            vertex.getCoordinate()
        );

        NamedArea okArea = null;
        Geometry polygon;

        for (NamedArea a : area.getAreas()) {
            polygon = a.getPolygon();
            Geometry intersection = line.intersection(polygon);
            if (intersection instanceof LineString) {
                LineString line2 = (LineString)intersection;
                if (line2.getNumPoints() != 2) {
                    continue;
                }
                Coordinate p1 = line2.getCoordinateN(0);
                Coordinate p2 = line2.getCoordinateN(1);
                double length2 = SphericalDistanceLibrary.distance(p1, p2);

                if (length - length2 < 0.000001) {
                    okArea = a;
                    break;
                }
            }
        }
        if (okArea != null) {
            linkCount++;
            I18NString name = new LocalizedString("", new OSMWithTags());
            edgeFactory.createAreaEdge(
                (IntersectionVertex) splitterVertex,
                (IntersectionVertex) vertex,
                line,
                name,
                length,
                okArea.getPermission(),
                false,
                area
            );
            edgeFactory.createAreaEdge(
                (IntersectionVertex) vertex,
                (IntersectionVertex) splitterVertex,
                line,
                name,
                length,
                okArea.getPermission(),
                true,
                area
            );
        }
      }
    }
    LOG.debug("added {} area edges to link a stop", linkCount);
  }
}
