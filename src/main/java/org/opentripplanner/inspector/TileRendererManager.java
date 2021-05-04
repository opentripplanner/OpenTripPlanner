package org.opentripplanner.inspector;

import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opentripplanner.common.geometry.MapTile;
import org.opentripplanner.api.resource.GraphInspectorTileResource;
import org.opentripplanner.inspector.TileRenderer.TileRenderContext;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Process slippy map tile rendering requests. Get the tile renderer for the given layer, setup a
 * tile rendering context (bounding box, image graphic context, affine transform, etc...) and call
 * the renderer to paint the tile.
 * 
 * @see GraphInspectorTileResource
 * @see TileRenderer
 * 
 * @author laurent
 * 
 */
public class TileRendererManager {

    private static final Logger LOG = LoggerFactory.getLogger(TileRendererManager.class);

    private Map<String, TileRenderer> renderers = new HashMap<String, TileRenderer>();

    private Graph graph;

    public TileRendererManager(Graph graph) {
        this.graph = graph;

        // Register layers.
        renderers.put("bike-safety", new EdgeVertexTileRenderer(new BikeSafetyEdgeRenderer()));
        renderers.put("thru-traffic", new EdgeVertexTileRenderer(new NoThruTrafficEdgeRenderer()));
        renderers.put("traversal", new EdgeVertexTileRenderer(
                new TraversalPermissionsEdgeRenderer()));
        renderers.put("wheelchair", new EdgeVertexTileRenderer(new WheelchairEdgeRenderer()));
    }

    public void registerRenderer(String layer, TileRenderer tileRenderer) {
        renderers.put(layer, tileRenderer);
    }

    public BufferedImage renderTile(final MapTile mapTile, String layer) {

        TileRenderContext context = new TileRenderContext() {
            @Override
            public Envelope expandPixels(double marginXPixels, double marginYPixels) {
                Envelope retval = new Envelope(bbox);
                retval.expandBy(
                        marginXPixels / mapTile.width * (bbox.getMaxX() - bbox.getMinX()),
                        marginYPixels / mapTile.height * (bbox.getMaxY() - bbox.getMinY()));
                return retval;
            }
        };

        context.graph = graph;

        TileRenderer renderer = renderers.get(layer);
        if (renderer == null)
            throw new IllegalArgumentException("Unknown layer: " + layer);

        // The best place for caching tiles may be here
        BufferedImage image = new BufferedImage(
            mapTile.width, mapTile.height,
                renderer.getColorModel());
        context.graphics = image.createGraphics();
        Envelope2D trbb = mapTile.bbox;
        context.bbox = new Envelope(trbb.x, trbb.x + trbb.width, trbb.y, trbb.y + trbb.height);
        context.transform = new AffineTransformation();
        double xScale = mapTile.width / trbb.width;
        double yScale = mapTile.height / trbb.height;

        context.transform.translate(-trbb.x, -trbb.y - trbb.height);
        context.transform.scale(xScale, -yScale);
        context.metersPerPixel = Math.toRadians(trbb.height) * 6371000 / mapTile.height;
        context.tileWidth = mapTile.width;
        context.tileHeight = mapTile.height;

        long start = System.currentTimeMillis();
        renderer.renderTile(context);
        LOG.debug("Rendered tile at {},{} in {} ms", mapTile.bbox.y, mapTile.bbox.x,
                System.currentTimeMillis() - start);
        return image;
    }

    /**
     * Gets all renderers
     * 
     * Used to return list of renderers to client.
     * Could be also used to show legend.
     * @return 
     */
    public Map<String, TileRenderer> getRenderers() {
        return renderers;
    }
}