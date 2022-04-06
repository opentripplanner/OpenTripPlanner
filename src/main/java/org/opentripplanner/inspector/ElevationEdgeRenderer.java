package org.opentripplanner.inspector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

class ElevationEdgeRenderer implements EdgeVertexRenderer {

  private final ScalarColorPalette colorPalette;

  ElevationEdgeRenderer(Graph graph) {
    if (graph.hasElevation) {
      colorPalette =
        new DefaultScalarColorPalette(
          graph.minElevation,
          (graph.minElevation + graph.maxElevation) / 2,
          graph.maxElevation
        );
    } else {
      colorPalette = new DefaultScalarColorPalette(0, 0, 0);
    }
  }

  @Override
  public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
    return true;
  }

  @Override
  public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
    var elevation = findElevationForVertex(v);
    if (elevation != null) {
      attrs.color = colorPalette.getColor(elevation);
      attrs.label = elevation.toString();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean hasEdgeSegments(Edge edge) {
    return true;
  }

  @Override
  public Iterable<T2<Double, Color>> edgeSegments(Edge edge) {
    if (edge instanceof StreetEdge streetEdge) {
      if (streetEdge.hasElevationExtension()) {
        var edgeLength = edge.getDistanceMeters();
        var color = Color.DARK_GRAY;

        var t2 = new ArrayList<T2<Double, Color>>();
        var profile = streetEdge.getElevationProfile();
        for (int i = 0; i < profile.size(); ++i) {
          var point = profile.getCoordinate(i);
          if (i != 0) {
            t2.add(new T2<>(point.x / edgeLength, color));
          }
          color = colorPalette.getColor(point.y);
        }
        return t2;
      } else {
        return List.of(new T2<>(1.0d, Color.GRAY));
      }
    } else {
      return List.of(new T2<>(1.0d, Color.LIGHT_GRAY));
    }
  }

  private Double findElevationForVertex(Vertex v) {
    return Stream
      .concat(
        v
          .getIncomingStreetEdges()
          .stream()
          .filter(StreetEdge::hasElevationExtension)
          .map(streetEdge ->
            streetEdge
              .getElevationProfile()
              .getCoordinate(streetEdge.getElevationProfile().size() - 1)
              .y
          ),
        v
          .getOutgoingStreetEdges()
          .stream()
          .filter(StreetEdge::hasElevationExtension)
          .map(streetEdge -> streetEdge.getElevationProfile().getCoordinate(0).y)
      )
      .findAny()
      .orElse(null);
  }

  @Override
  public String getName() {
    return "Elevation";
  }
}
