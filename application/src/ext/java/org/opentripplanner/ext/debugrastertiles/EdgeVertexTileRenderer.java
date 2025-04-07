package org.opentripplanner.ext.debugrastertiles;

import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.TextStroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.awt.IdentityPointTransformation;
import org.locationtech.jts.awt.PointShapeFactory;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.linearref.LengthLocationMap;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.buffer.OffsetCurveBuilder;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A TileRenderer implementation which get all edges/vertex in the bounding box of the tile, and
 * call a EdgeVertexRenderer for getting rendering attributes of each (color, string label...).
 *
 * @author laurent
 */
public class EdgeVertexTileRenderer implements TileRenderer {

  private final EdgeVertexRenderer evRenderer;

  public EdgeVertexTileRenderer(EdgeVertexRenderer evRenderer) {
    this.evRenderer = evRenderer;
  }

  @Override
  public int getColorModel() {
    return BufferedImage.TYPE_INT_ARGB;
  }

  @Override
  public void renderTile(TileRenderContext context) {
    float lineWidth = (float) (1.0f + 3.0f / Math.sqrt(context.metersPerPixel));

    // Grow a bit the envelope to prevent rendering glitches between tiles
    Envelope bboxWithMargins = context.expandPixels(lineWidth * 2.0, lineWidth * 2.0);

    Collection<Vertex> vertices = context.graph
      .findVertices(bboxWithMargins)
      .stream()
      .sorted(evRenderer::vertexSorter)
      .toList();

    Collection<Edge> edges = context.graph
      .findEdges(bboxWithMargins)
      .stream()
      .distinct()
      .sorted(evRenderer::edgeSorter)
      .toList();

    // Note: we do not use the transform inside the shapeWriter, but do it ourselves
    // since it's easier for the offset to work in pixel size.
    ShapeWriter shapeWriter = new ShapeWriter(
      new IdentityPointTransformation(),
      new PointShapeFactory.Point()
    );

    Stroke stroke = new BasicStroke(
      lineWidth * 1.4f,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_BEVEL
    );
    Stroke halfStroke = new BasicStroke(
      lineWidth * 0.6f + 1.0f,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_BEVEL
    );
    Stroke halfDashedStroke = new BasicStroke(
      lineWidth * 0.6f + 1.0f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_BEVEL,
      1.0f,
      new float[] { 4 * lineWidth, lineWidth },
      2 * lineWidth
    );
    Stroke arrowStroke = new ShapeStroke(
      new Polygon(new int[] { 0, 0, 30 }, new int[] { 0, 20, 10 }, 3),
      lineWidth / 2,
      5.0f * lineWidth,
      2.5f * lineWidth
    );
    BasicStroke thinStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);

    Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth));
    Font largeFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth * 1.5f));
    FontMetrics largeFontMetrics = context.graphics.getFontMetrics(largeFont);
    context.graphics.setFont(largeFont);
    context.graphics.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON
    );
    context.graphics.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    );

    BufferParameters bufParams = new BufferParameters();
    bufParams.setSingleSided(true);
    bufParams.setJoinStyle(BufferParameters.JOIN_BEVEL);

    // Render all edges
    for (Edge edge : edges) {
      Geometry edgeGeom = edge.getGeometry();
      boolean hasGeom = true;
      if (edgeGeom == null) {
        Coordinate[] coordinates = new Coordinate[] {
          edge.getFromVertex().getCoordinate(),
          edge.getToVertex().getCoordinate(),
        };
        edgeGeom = GeometryUtils.getGeometryFactory().createLineString(coordinates);
        hasGeom = false;
      }

      var evAttrsOpt = evRenderer.renderEdge(edge);

      if (evAttrsOpt.isEmpty()) {
        continue;
      }
      EdgeVisualAttributes evAttrs = evAttrsOpt.get();

      Geometry midLineGeom = context.transform.transform(edgeGeom);
      OffsetCurveBuilder offsetBuilder = new OffsetCurveBuilder(new PrecisionModel(), bufParams);
      Coordinate[] coords = offsetBuilder.getOffsetCurve(
        midLineGeom.getCoordinates(),
        lineWidth * 0.4
      );
      if (coords.length < 2) continue; // Can happen for very small edges (<1mm)
      LineString offsetLine = GeometryUtils.makeLineString(coords);
      Shape midLineShape = shapeWriter.toShape(midLineGeom);
      Shape offsetShape = shapeWriter.toShape(offsetLine);

      context.graphics.setStroke(hasGeom ? halfStroke : halfDashedStroke);

      if (evRenderer.hasEdgeSegments(edge)) {
        LocationIndexedLine line = new LocationIndexedLine(offsetLine);
        LengthLocationMap locater = new LengthLocationMap(offsetLine);
        var offsetLength = offsetLine.getLength();

        var previousLocation = line.getStartIndex();
        for (var it : evRenderer.edgeSegments(edge)) {
          var currentLocation = locater.getLocation(offsetLength * it.position());
          var segmentGeometry = line.extractLine(previousLocation, currentLocation);
          var segmentShape = shapeWriter.toShape(segmentGeometry);
          context.graphics.setColor(it.color());
          context.graphics.draw(segmentShape);

          previousLocation = currentLocation;
        }
      } else {
        context.graphics.setColor(evAttrs.color);
        context.graphics.draw(offsetShape);
      }
      if (lineWidth > 6.0f) {
        context.graphics.setColor(Color.WHITE);
        context.graphics.setStroke(arrowStroke);
        context.graphics.draw(offsetShape);
      }
      if (lineWidth > 4.0f) {
        context.graphics.setColor(Color.BLACK);
        context.graphics.setStroke(thinStroke);
        context.graphics.draw(midLineShape);
      }
      if (evAttrs.label != null && lineWidth > 8.0f) {
        context.graphics.setColor(Color.BLACK);
        context.graphics.setStroke(
          new TextStroke(
            "    " + evAttrs.label + "                              ",
            font,
            false,
            true
          )
        );
        context.graphics.draw(offsetShape);
      }
    }

    // Render all vertices
    for (Vertex vertex : vertices) {
      Point point = GeometryUtils.getGeometryFactory().createPoint(vertex.getCoordinate());
      var vvAttrsOp = evRenderer.renderVertex(vertex);

      if (vvAttrsOp.isEmpty()) {
        continue;
      }
      var vvAttrs = vvAttrsOp.get();

      Point tilePoint = (Point) context.transform.transform(point);
      Shape shape = shapeWriter.toShape(tilePoint);

      context.graphics.setColor(vvAttrs.color);
      context.graphics.setStroke(stroke);
      context.graphics.draw(shape);
      if (
        vvAttrs.label != null && lineWidth > 6.0f && context.bbox.contains(point.getCoordinate())
      ) {
        context.graphics.setColor(Color.BLACK);
        int labelWidth = largeFontMetrics.stringWidth(vvAttrs.label);
        /*
         * Poor man's solution: stay on the tile if possible. Otherwise the renderer would
         * need to expand the envelope by an unbounded amount (max label size).
         */
        double x = tilePoint.getX();
        if (x + labelWidth > context.tileWidth) x -= labelWidth;
        context.graphics.drawString(vvAttrs.label, (float) x, (float) tilePoint.getY());
      }
    }
  }

  public interface EdgeVertexRenderer {
    Comparator<Vertex> defaultVertexComparator = Comparator.comparing((Vertex v) ->
      v instanceof StreetVertex
    ).reversed();
    Comparator<Edge> defaultEdgeComparator = Comparator.comparing(
      (Edge e) -> e.getGeometry() != null
    ).thenComparing(e -> e instanceof StreetEdge);

    /**
     * @param e The edge being rendered.
     * @return  edge to render, or empty otherwise.
     */
    Optional<EdgeVisualAttributes> renderEdge(Edge e);

    /**
     * @param v The vertex being rendered.
     * @return  vertex to render, or empty otherwise.
     */
    Optional<VertexVisualAttributes> renderVertex(Vertex v);

    default boolean hasEdgeSegments(Edge edge) {
      return false;
    }

    default Iterable<EdgeSegmentColor> edgeSegments(Edge edge) {
      return List.of();
    }

    default int vertexSorter(Vertex v1, Vertex v2) {
      return defaultVertexComparator.compare(v1, v2);
    }

    default int edgeSorter(Edge e1, Edge e2) {
      return defaultEdgeComparator.compare(e1, e2);
    }
  }

  public record EdgeVisualAttributes(Color color, String label) {
    public static Optional<EdgeVisualAttributes> optional(Color color, String label) {
      return Optional.of(new EdgeVisualAttributes(color, label));
    }
  }

  public record VertexVisualAttributes(Color color, String label) {
    public static Optional<VertexVisualAttributes> optional(Color color, String label) {
      return Optional.of(new VertexVisualAttributes(color, label));
    }
  }

  record EdgeSegmentColor(Double position, Color color) {}
}
