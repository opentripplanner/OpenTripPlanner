package org.opentripplanner.inspector;

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
import java.util.stream.Collectors;
import org.locationtech.jts.awt.IdentityPointTransformation;
import org.locationtech.jts.awt.PointShapeFactory;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.buffer.OffsetCurveBuilder;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * A TileRenderer implementation which get all edges/vertex in the bounding box of the tile, and
 * call a EdgeVertexRenderer for getting rendering attributes of each (color, string label...).
 * 
 * @author laurent
 */
public class EdgeVertexTileRenderer implements TileRenderer {

    public class EdgeVisualAttributes {

        public Color color;

        public String label;
    }

    public class VertexVisualAttributes {

        public Color color;

        public String label;
    }

    public interface EdgeVertexRenderer {

        /**
         * @param e The edge being rendered.
         * @param attrs The edge visual attributes to fill-in.
         * @return True to render this edge, false otherwise.
         */
        boolean renderEdge(Edge e, EdgeVisualAttributes attrs);

        /**
         * @param v The vertex being rendered.
         * @param attrs The vertex visual attributes to fill-in.
         * @return True to render this vertex, false otherwise.
         */
        boolean renderVertex(Vertex v, VertexVisualAttributes attrs);

        /**
         * Name of this tile Render which would be shown in frontend
         *
         * @return Name of tile render
         */
        String getName();

        default int vertexSorter(Vertex v1, Vertex v2) {
            return defaultVertexComparator.compare(v1, v2);
        }

        default int edgeSorter(Edge e1, Edge e2) {
            return defaultEdgeComparator.compare(e1, e2);
        }


        Comparator<Vertex> defaultVertexComparator = Comparator.comparing((Vertex v) -> v instanceof StreetVertex)
                .reversed();

        Comparator<Edge> defaultEdgeComparator = Comparator.comparing((Edge e) -> e.getGeometry() != null)
                .thenComparing(e -> e instanceof StreetEdge);
    }

    @Override
    public int getColorModel() {
        return BufferedImage.TYPE_INT_ARGB;
    }

    private EdgeVertexRenderer evRenderer;

    public EdgeVertexTileRenderer(EdgeVertexRenderer evRenderer) {
        this.evRenderer = evRenderer;
    }

    @Override
    public String getName() {
        return evRenderer.getName();
    }

    @Override
    public void renderTile(TileRenderContext context) {

        float lineWidth = (float) (1.0f + 3.0f / Math.sqrt(context.metersPerPixel));

        // Grow a bit the envelope to prevent rendering glitches between tiles
        Envelope bboxWithMargins = context.expandPixels(lineWidth * 2.0, lineWidth * 2.0);

        Collection<Vertex> vertices = context.graph.getStreetIndex()
                .getVerticesForEnvelope(bboxWithMargins)
                .stream()
                .sorted(evRenderer::vertexSorter)
                .collect(Collectors.toList());

        Collection<Edge> edges = context.graph.getStreetIndex().getEdgesForEnvelope(bboxWithMargins)
                .stream()
                .distinct()
                .sorted(evRenderer::edgeSorter)
                .collect(Collectors.toList());

        // Note: we do not use the transform inside the shapeWriter, but do it ourselves
        // since it's easier for the offset to work in pixel size.
        ShapeWriter shapeWriter = new ShapeWriter(new IdentityPointTransformation(),
                new PointShapeFactory.Point());
        GeometryFactory geomFactory = new GeometryFactory();

        Stroke stroke = new BasicStroke(lineWidth * 1.4f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);
        Stroke halfStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);
        Stroke halfDashedStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1.0f, new float[] { 4 * lineWidth, lineWidth },
                2 * lineWidth);
        Stroke arrowStroke = new ShapeStroke(new Polygon(new int[] { 0, 0, 30 }, new int[] { 0, 20,
                10 }, 3), lineWidth / 2, 5.0f * lineWidth, 2.5f * lineWidth);
        BasicStroke thinStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_BEVEL);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth));
        Font largeFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth * 1.5f));
        FontMetrics largeFontMetrics = context.graphics.getFontMetrics(largeFont);
        context.graphics.setFont(largeFont);
        context.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        context.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferParameters bufParams = new BufferParameters();
        bufParams.setSingleSided(true);
        bufParams.setJoinStyle(BufferParameters.JOIN_BEVEL);

        // Render all edges
        EdgeVisualAttributes evAttrs = new EdgeVisualAttributes();
        for (Edge edge : edges) {
            evAttrs.color = null;
            evAttrs.label = null;
            Geometry edgeGeom = edge.getGeometry();
            boolean hasGeom = true;
            if (edgeGeom == null) {
                Coordinate[] coordinates = new Coordinate[] { edge.getFromVertex().getCoordinate(),
                        edge.getToVertex().getCoordinate() };
                edgeGeom = GeometryUtils.getGeometryFactory().createLineString(coordinates);
                hasGeom = false;
            }

            boolean render = evRenderer.renderEdge(edge, evAttrs);
            if (!render)
                continue;

            Geometry midLineGeom = context.transform.transform(edgeGeom);
            OffsetCurveBuilder offsetBuilder = new OffsetCurveBuilder(new PrecisionModel(),
                    bufParams);
            Coordinate[] coords = offsetBuilder.getOffsetCurve(midLineGeom.getCoordinates(),
                    lineWidth * 0.4);
            if (coords.length < 2)
                continue; // Can happen for very small edges (<1mm)
            LineString offsetLine = geomFactory.createLineString(coords);
            Shape midLineShape = shapeWriter.toShape(midLineGeom);
            Shape offsetShape = shapeWriter.toShape(offsetLine);

            context.graphics.setStroke(hasGeom ? halfStroke : halfDashedStroke);
            context.graphics.setColor(evAttrs.color);
            context.graphics.draw(offsetShape);
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
                context.graphics.setStroke(new TextStroke("    " + evAttrs.label
                        + "                              ", font, false, true));
                context.graphics.draw(offsetShape);
            }
        }

        // Render all vertices
        VertexVisualAttributes vvAttrs = new VertexVisualAttributes();
        for (Vertex vertex : vertices) {
            vvAttrs.color = null;
            vvAttrs.label = null;
            Point point = geomFactory.createPoint(new Coordinate(vertex.getLon(), vertex.getLat()));
            boolean render = evRenderer.renderVertex(vertex, vvAttrs);
            if (!render)
                continue;

            Point tilePoint = (Point) context.transform.transform(point);
            Shape shape = shapeWriter.toShape(tilePoint);

            context.graphics.setColor(vvAttrs.color);
            context.graphics.setStroke(stroke);
            context.graphics.draw(shape);
            if (vvAttrs.label != null && lineWidth > 6.0f
                    && context.bbox.contains(point.getCoordinate())) {
                context.graphics.setColor(Color.BLACK);
                int labelWidth = largeFontMetrics.stringWidth(vvAttrs.label);
                /*
                 * Poor man's solution: stay on the tile if possible. Otherwise the renderer would
                 * need to expand the envelope by an unbounded amount (max label size).
                 */
                double x = tilePoint.getX();
                if (x + labelWidth > context.tileWidth)
                    x -= labelWidth;
                context.graphics.drawString(vvAttrs.label, (float) x, (float) tilePoint.getY());
            }
        }
    }
}