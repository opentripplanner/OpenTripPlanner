package org.opentripplanner.ext.debugrastertiles;

import java.awt.Graphics2D;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opentripplanner.routing.graph.Graph;

/**
 * Interface for a slippy map tile renderer.
 *
 * @author laurent
 */
public interface TileRenderer {
  /** Return the BufferedImage color model the renderer would like to use */
  int getColorModel();

  /** Implementation of the tile rendering */
  void renderTile(TileRenderContext context);

  /** Gets descriptive name of this Tile Render */
  String getName();

  /**
   * Context used for rendering a tile.
   */
  abstract class TileRenderContext {

    /** Graphics where to paint tile to, in pixel CRS (no transform set) */
    public Graphics2D graphics;

    /** The JTS transform that convert from WGS84 CRS to pixel CRS */
    public AffineTransformation transform;

    /** The graph being processed */
    public Graph graph;

    /** Bounding box of the rendered tile in WGS84 CRS, w/o margins */
    public Envelope bbox;

    /** Ground pixel density inverse */
    public double metersPerPixel;

    /** Tile size in pixels */
    public int tileWidth, tileHeight;

    /** Expand the bounding box to add some margins, in pixel size. */
    public abstract Envelope expandPixels(double marginXPixels, double marginYPixels);
  }
}
