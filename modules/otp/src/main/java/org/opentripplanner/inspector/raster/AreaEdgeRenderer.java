package org.opentripplanner.inspector.raster;

import java.awt.Color;
import java.util.Iterator;
import java.util.Optional;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeList;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitBoardingAreaVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitPathwayNodeVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Render OSM areas only. Show area edge vertices and OSM boarding locations. Highlight visiblity point vertices.
 *
 */
public class AreaEdgeRenderer implements EdgeVertexRenderer {

  private static final Color AREA_COLOR = new Color(0.2f, 0.4f, 0.6f);
  private static final Color OSM_BOARDING_LOCATION_VERTEX_COLOR = new Color(23, 160, 234);
  private static final Color TRANSIT_STOP_COLOR_VERTEX = new Color(0.0f, 0.0f, 0.8f);
  private static final Color AREA_COLOR_VERTEX = Color.DARK_GRAY;
  private static final Color VISIBILITY_COLOR_VERTEX = Color.RED;

  private enum vxType {
    VISIBILITY_VERTEX,
    AREA_VERTEX,
    OTHER_VERTEX,
  }

  private vxType _getAreaVertexType(Vertex v, Iterator<Edge> iterator) {
    vxType val = vxType.OTHER_VERTEX;

    while (iterator.hasNext()) {
      if (iterator.next() instanceof AreaEdge ae) {
        AreaEdgeList list = ae.getArea();
        if (list.visibilityVertices().contains(v)) {
          return vxType.VISIBILITY_VERTEX;
        }
        val = vxType.AREA_VERTEX;
      }
    }
    return val;
  }

  private vxType getAreaVertexType(Vertex v) {
    vxType type1 = _getAreaVertexType(v, v.getOutgoing().iterator());
    if (type1 == vxType.VISIBILITY_VERTEX) {
      return vxType.VISIBILITY_VERTEX;
    }
    vxType type2 = _getAreaVertexType(v, v.getIncoming().iterator());
    if (type2 == vxType.VISIBILITY_VERTEX) {
      return vxType.VISIBILITY_VERTEX;
    }
    if (type1 == vxType.AREA_VERTEX || type2 == vxType.AREA_VERTEX) {
      return vxType.AREA_VERTEX;
    }
    return vxType.OTHER_VERTEX;
  }

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (e instanceof AreaEdge ae) {
      return EdgeVisualAttributes.optional(AREA_COLOR, "");
    }
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (v instanceof OsmBoardingLocationVertex osmV) {
      return VertexVisualAttributes.optional(
        OSM_BOARDING_LOCATION_VERTEX_COLOR,
        "OSM refs" + osmV.references
      );
    } else if (
      v instanceof TransitStopVertex ||
      v instanceof TransitEntranceVertex ||
      v instanceof TransitPathwayNodeVertex ||
      v instanceof TransitBoardingAreaVertex
    ) {
      return VertexVisualAttributes.optional(TRANSIT_STOP_COLOR_VERTEX, v.getDefaultName());
    } else {
      vxType type = getAreaVertexType(v);
      if (type == vxType.VISIBILITY_VERTEX) {
        return VertexVisualAttributes.optional(VISIBILITY_COLOR_VERTEX, null);
      } else if (type == vxType.AREA_VERTEX) {
        return VertexVisualAttributes.optional(AREA_COLOR_VERTEX, null);
      }
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "Areas";
  }
}
