package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;

import java.util.ArrayList;
import java.util.List;

// TODO Temporary code until we refactor the WalkableAreaBuilder (#3152)
class AreaVisibilityAdjuster {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  private static final StreetEdgeFactory edgeFactory = new DefaultStreetEdgeFactory();

  // Link to all vertices in area/platform
  static void linkTransitToAreaVertices(Vertex splitterVertex, AreaEdgeList area) {
    List<Vertex> vertices = new ArrayList<>();

    for (AreaEdge areaEdge : area.getEdges()) {
      if (!vertices.contains(areaEdge.getToVertex())) { vertices.add(areaEdge.getToVertex()); }
      if (!vertices.contains(areaEdge.getFromVertex())) { vertices.add(areaEdge.getFromVertex()); }
    }

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
        I18NString name = new LocalizedString("", new OSMWithTags());

        edgeFactory.createAreaEdge(
            (IntersectionVertex) splitterVertex,
            (IntersectionVertex) vertex,
            line,
            name,
            length,
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            false,
            area
        );
        edgeFactory.createAreaEdge(
            (IntersectionVertex) vertex,
            (IntersectionVertex) splitterVertex,
            line,
            name,
            length,
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            false,
            area
        );
      }
    }
  }
}
