package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeSegmentColor;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;

class ElevationEdgeRenderer implements EdgeVertexRenderer {

  private final ScalarColorPalette colorPalette;

  ElevationEdgeRenderer(Graph graph) {
    if (graph.hasElevation) {
      colorPalette = new DefaultScalarColorPalette(
        graph.minElevation,
        (graph.minElevation + graph.maxElevation) / 2,
        graph.maxElevation
      );
    } else {
      colorPalette = new DefaultScalarColorPalette(0, 0, 0);
    }
  }

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    var elevation = findElevationForVertex(v);
    if (elevation != null) {
      return Optional.of(
        new VertexVisualAttributes(colorPalette.getColor(elevation), elevation.toString())
      );
    } else {
      return Optional.empty();
    }
  }

  @Override
  public String getName() {
    return "Elevation";
  }

  @Override
  public boolean hasEdgeSegments(Edge edge) {
    return true;
  }

  @Override
  public Iterable<EdgeSegmentColor> edgeSegments(Edge edge) {
    if (edge instanceof StreetEdge streetEdge) {
      if (streetEdge.hasElevationExtension()) {
        var edgeLength = edge.getDistanceMeters();
        var color = Color.DARK_GRAY;

        var list = new ArrayList<EdgeSegmentColor>();
        var profile = streetEdge.getElevationProfile();
        for (int i = 0; i < profile.size(); ++i) {
          var point = profile.getCoordinate(i);
          if (i != 0) {
            list.add(new EdgeSegmentColor(point.x / edgeLength, color));
          }
          color = colorPalette.getColor(point.y);
        }
        return list;
      } else {
        return List.of(new EdgeSegmentColor(1.0d, Color.GRAY));
      }
    } else {
      return List.of(new EdgeSegmentColor(1.0d, Color.LIGHT_GRAY));
    }
  }

  private Double findElevationForVertex(Vertex v) {
    return Stream.concat(
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
}
