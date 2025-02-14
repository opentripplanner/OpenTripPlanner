package org.opentripplanner.ext.debugrastertiles;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opentripplanner.ext.debugrastertiles.TileRenderer.TileRenderContext;
import org.opentripplanner.ext.debugrastertiles.api.resource.DebugRasterTileResource;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process slippy map tile rendering requests. Get the tile renderer for the given layer, setup a
 * tile rendering context (bounding box, image graphic context, affine transform, etc...) and call
 * the renderer to paint the tile.
 *
 * @author laurent
 * @see DebugRasterTileResource
 * @see TileRenderer
 */
public class TileRendererManager {

  private static final Logger LOG = LoggerFactory.getLogger(TileRendererManager.class);

  private final Map<String, TileRenderer> renderers = new HashMap<>();

  private final Graph graph;

  public TileRendererManager(Graph graph, WheelchairPreferences wheelchairPreferences) {
    this.graph = graph;

    // Register layers.
    renderers.put("bike-safety", new EdgeVertexTileRenderer(new BikeSafetyEdgeRenderer()));
    renderers.put("walk-safety", new EdgeVertexTileRenderer(new WalkSafetyEdgeRenderer()));
    renderers.put("thru-traffic", new EdgeVertexTileRenderer(new NoThruTrafficEdgeRenderer()));
    renderers.put("traversal", new EdgeVertexTileRenderer(new TraversalPermissionsEdgeRenderer()));
    renderers.put(
      "wheelchair",
      new EdgeVertexTileRenderer(new WheelchairEdgeRenderer(wheelchairPreferences))
    );
    renderers.put("elevation", new EdgeVertexTileRenderer(new ElevationEdgeRenderer(graph)));
  }

  public BufferedImage renderTile(final MapTile mapTile, String layer) {
    TileRenderContext context = new TileRenderContext() {
      @Override
      public Envelope expandPixels(double marginXPixels, double marginYPixels) {
        Envelope retval = new Envelope(bbox);
        retval.expandBy(
          marginXPixels / mapTile.width() * (bbox.getMaxX() - bbox.getMinX()),
          marginYPixels / mapTile.height() * (bbox.getMaxY() - bbox.getMinY())
        );
        return retval;
      }
    };

    context.graph = graph;

    TileRenderer renderer = renderers.get(layer);
    if (renderer == null) throw new IllegalArgumentException("Unknown layer: " + layer);

    // The best place for caching tiles may be here
    BufferedImage image = new BufferedImage(
      mapTile.width(),
      mapTile.height(),
      renderer.getColorModel()
    );
    context.graphics = image.createGraphics();
    context.bbox = mapTile.bbox();
    context.transform = new AffineTransformation();
    double xScale = mapTile.width() / context.bbox.getWidth();
    double yScale = mapTile.height() / context.bbox.getHeight();

    context.transform.translate(
      -context.bbox.getMinX(),
      -context.bbox.getMinY() - context.bbox.getHeight()
    );
    context.transform.scale(xScale, -yScale);
    context.metersPerPixel = Math.toRadians(context.bbox.getHeight()) * 6371000 / mapTile.height();
    context.tileWidth = mapTile.width();
    context.tileHeight = mapTile.height();

    long start = System.currentTimeMillis();
    renderer.renderTile(context);
    LOG.debug("Rendered tile at {} in {} ms", mapTile.bbox(), System.currentTimeMillis() - start);
    return image;
  }
}
